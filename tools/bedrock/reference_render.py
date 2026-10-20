"""Faithful offline repro of the pack's own 1.8.9 renderer (DeltaForce Melee source).

This is the authoritative convention for the melee code: it matches the Java in
me.Padej_.soupapi.bedrock one-to-one, and was validated against the pack author's own in-game
footage (the weapon lands in the same place, pose and angle). If a future pack renders wrong,
reproduce it here first. The older bedrock.py / fp.py in this folder predate the convention and
are kept only for the cube-rotation icon checks.

Bedrock -> Java at load: negate pivot.x, restRot.x and .y, animation pos.x and rot.x/.y, and
mirror cube origin.x; no runtime X flip. A pose is per-bone globals (Z*Y*X rotations), and the
view is scale(1/16) * inverse(global(idle_view) * T(pivot)) so idle_view sits at the eye.
"""

import json, math, os, glob
import numpy as np

def ident(): return np.eye(4)
def T(x,y,z):
    m=np.eye(4); m[0,3],m[1,3],m[2,3]=x,y,z; return m
def S(x,y,z):
    m=np.eye(4); m[0,0],m[1,1],m[2,2]=x,y,z; return m
def Rx(d):
    a=math.radians(d); c,s=math.cos(a),math.sin(a); m=np.eye(4)
    m[1,1]=c; m[1,2]=-s; m[2,1]=s; m[2,2]=c; return m   # column-major Java rot[5]=c,rot[6]=s,rot[9]=-s -> row form
def Ry(d):
    a=math.radians(d); c,s=math.cos(a),math.sin(a); m=np.eye(4)
    m[0,0]=c; m[0,2]=s; m[2,0]=-s; m[2,2]=c; return m
def Rz(d):
    a=math.radians(d); c,s=math.cos(a),math.sin(a); m=np.eye(4)
    m[0,0]=c; m[0,1]=-s; m[1,0]=s; m[1,1]=c; return m
def RZYX(rx,ry,rz):   # matrix = Rz*Ry*Rx (post-multiply order in Java: rotateZ then Y then X)
    return Rz(rz)@Ry(ry)@Rx(rx)

# NOTE on sign conventions: Java Mat4 is column-major m[col*4+row]; rotateX sets
# rot[5]=c,rot[6]=s,rot[9]=-s,rot[10]=c meaning (col1row2)=s ... but to keep this readable I use the
# standard right-handed matrices above and verify by rendering. If a sign is off the picture shows it.

def vec3(e, d=0.0):
    if e is None: return [d,d,d]
    if isinstance(e,(int,float)): return [float(e)]*3
    if isinstance(e,list):
        if len(e)==1: return [float(e[0])]*3
        return [float(e[0]),float(e[1]),float(e[2])]
    return [d,d,d]

class Model:
    def __init__(self, path):
        geo=json.load(open(path))['minecraft:geometry'][0]
        self.texW=geo['description']['texture_width']; self.texH=geo['description']['texture_height']
        bones=geo['bones']; self.n=len(bones)
        self.names=[]; self.parent=[]; self.pivot=[]; self.restRot=[]; self.quads=[]
        idx={}
        pnames=[]
        for i,b in enumerate(bones):
            self.names.append(b['name']); idx[b['name']]=i
            pnames.append(b.get('parent'))
            p=vec3(b.get('pivot'),0.0); self.pivot.append([-p[0],p[1],p[2]])
            r=vec3(b.get('rotation'),0.0); self.restRot.append([-r[0],-r[1],r[2]])
            q=[]
            for c in b.get('cubes',[]): self._cube(c,q)
            self.quads.append(q)
        self.parent=[idx.get(pn,-1) if pn else -1 for pn in pnames]
        self.idx=idx
        # topo order
        order=[]; done=[False]*self.n
        while len(order)<self.n:
            prog=False
            for i in range(self.n):
                if done[i]: continue
                if self.parent[i]<0 or done[self.parent[i]]:
                    done[i]=True; order.append(i); prog=True
            if not prog:
                for i in range(self.n):
                    if not done[i]: self.parent[i]=-1
        self.order=order
    def indexOf(self,name): return self.idx.get(name,-1)
    def _cube(self,c,out):
        o=vec3(c.get('origin'),0.0); s=vec3(c.get('size'),0.0)
        inf=float(c.get('inflate',0.0))
        x0=-(o[0]+s[0])-inf; y0=o[1]-inf; z0=o[2]-inf
        x1=-(o[0]+s[0])+s[0]+inf; y1=o[1]+s[1]+inf; z1=o[2]+s[2]+inf
        P1=[x0,y0,z0];P2=[x0,y0,z1];P3=[x0,y1,z0];P4=[x0,y1,z1]
        P5=[x1,y0,z0];P6=[x1,y0,z1];P7=[x1,y1,z0];P8=[x1,y1,z1]
        m=None
        if 'rotation' in c:
            cp=vec3(c.get('pivot'),0.0); cr=vec3(c.get('rotation'),0.0)
            m=T(-cp[0],cp[1],cp[2])@RZYX(-cr[0],-cr[1],cr[2])@T(cp[0],-cp[1],-cp[2])
        uv=c.get('uv')
        if not isinstance(uv,dict): return
        for name,verts in (('west',[P4,P3,P1,P2]),('east',[P7,P8,P6,P5]),('north',[P3,P7,P5,P1]),
                           ('south',[P8,P4,P2,P6]),('up',[P4,P8,P7,P3]),('down',[P1,P5,P6,P2])):
            f=uv.get(name)
            if not f or 'uv' not in f: continue
            u1,v1=f['uv']; us,vs=f.get('uv_size',[0,0])
            uvs=[[(u1+us)/self.texW,v1/self.texH],[u1/self.texW,v1/self.texH],
                 [u1/self.texW,(v1+vs)/self.texH],[(u1+us)/self.texW,(v1+vs)/self.texH]]
            pts=[]
            for vtx in verts:
                if m is not None:
                    q=m@np.array([vtx[0],vtx[1],vtx[2],1.0]); pts.append(q[:3])
                else: pts.append(np.array(vtx,float))
            out.append((pts,uvs))
    def bounds(self,bone):
        q=self.quads[bone]
        if not q: return None
        lo=np.array([1e9]*3); hi=np.array([-1e9]*3)
        for pts,_ in q:
            for p in pts:
                lo=np.minimum(lo,p); hi=np.maximum(hi,p)
        return lo,hi

def globals_of(model, pose):
    G=[None]*model.n
    for bi in model.order:
        par=model.parent[bi]
        g=G[par].copy() if par>=0 else np.eye(4)
        o=bi*9; pv=model.pivot[bi]; rr=model.restRot[bi]
        g=g@T(pose[o],pose[o+1],pose[o+2])
        g=g@T(pv[0],pv[1],pv[2])
        g=g@RZYX(rr[0]+pose[o+3],rr[1]+pose[o+4],rr[2]+pose[o+5])
        g=g@S(pose[o+6],pose[o+7],pose[o+8])
        g=g@T(-pv[0],-pv[1],-pv[2])
        G[bi]=g
    return G

def idle_pose(model, anim):
    pose=np.zeros(model.n*9)
    for i in range(model.n): pose[i*9+6]=pose[i*9+7]=pose[i*9+8]=1.0
    si=anim['animations'].get('static_idle')
    if si and 'bones' in si:
        for bname,ch in si['bones'].items():
            bi=model.indexOf(bname)
            if bi<0: continue
            o=bi*9
            for key,base,neg in (('position',0,(True,False,False)),('rotation',3,(True,True,False)),('scale',6,(False,False,False))):
                if key not in ch: continue
                v=ch[key]
                val=_first(v)
                if key=='scale' and val is None: continue
                if val is None: val=[0,0,0]
                vv=list(val)
                if neg[0]: vv[0]=-vv[0]
                if neg[1]: vv[1]=-vv[1]
                pose[o+base]=vv[0]; pose[o+base+1]=vv[1]; pose[o+base+2]=vv[2]
    return pose

def _first(v):
    if isinstance(v,list): return v if len(v)==3 else [v[0]]*3
    if isinstance(v,(int,float)): return [v]*3
    if isinstance(v,dict):
        # keyframed: take t=0 (post of first key) — static_idle is constant anyway
        ks=sorted(v.keys(), key=lambda s: float(s))
        kf=v[ks[0]]
        if isinstance(kf,dict): kf=kf.get('post') or kf.get('vector') or kf.get('pre')
        return _first(kf)
    return None

def view_matrix(model, G, idleIdx):
    if idleIdx>=0:
        pv=model.pivot[idleIdx]
        inner=G[idleIdx]@T(pv[0],pv[1],pv[2])
        inv=np.linalg.inv(inner)
    else:
        inv=np.eye(4)
    return S(1/16,1/16,1/16)@inv
