package io.github.astelev.around_planet.moon;
/** Thread-safe height/biome field shared by terrain, biome lookup and orbital albedo. X is periodic. */
public record MoonTerrain(double circumference,long seed) {
 public enum Region {PLAINS,MARE,RIM,BASIN}
 public record Sample(int height,Region region,boolean ice){}
 public MoonTerrain {if(!Double.isFinite(circumference)||circumference<=0)throw new IllegalArgumentException("Moon circumference");}
 public static long worldSeed(long seed){return mix(seed^0x6d6f6f6e5f617032L);}
 private static long mix(long x){x=(x^(x>>>30))*0xbf58476d1ce4e5b9L;x=(x^(x>>>27))*0x94d049bb133111ebL;return x^(x>>>31);}
 private double random(int x,int z,int salt){return (mix(seed+x*0x632be59bd9b4e019L+z*0x9e3779b97f4a7c15L+salt)>>>11)*0x1.0p-53;}
 private static double smooth(double a,double b,double x){double t=Math.clamp((x-a)/(b-a),0,1);return t*t*(3-2*t);}
 private double noise(double x,double z,int cells,int salt){double xx=x/circumference*cells,zz=z/circumference*cells;int ix=(int)Math.floor(xx),iz=(int)Math.floor(zz);double tx=smooth(0,1,xx-ix),tz=smooth(0,1,zz-iz);double a=random(Math.floorMod(ix,cells),iz,salt),b=random(Math.floorMod(ix+1,cells),iz,salt),c=random(Math.floorMod(ix,cells),iz+1,salt),d=random(Math.floorMod(ix+1,cells),iz+1,salt);return 2*((a+(b-a)*tx)*(1-tz)+(c+(d-c)*tx)*tz)-1;}
 private double base(double x,double z){double sea=smooth(-.15,.2,noise(x,z,8,11));double plains=88+42*noise(x,z,32,12)+17*noise(x,z,96,13)+4*noise(x,z,384,14),mare=60+6*noise(x,z,96,15)+3*noise(x,z,384,16);return plains*(1-sea)+mare*sea;}
 /** Relative crater relief: depressed floor, raised ring and a central peak only in large impacts. */
 public static double relief(double t,double radius){double depth=Math.min(65,18+radius*.10),rim=Math.min(85,22+radius*.12);double bowl=-depth*(1-smooth(.60,.91,t)),ring=rim*Math.exp(-Math.pow((t-1)/.14,2)),peak=radius>=320?(depth+25)*Math.exp(-Math.pow(t/.18,2)):0;return bowl+ring+peak;}
 public Sample sample(double worldX,double z){
  double x=worldX-Math.floor(worldX/circumference)*circumference;double height=base(x,z);Region region=noise(x,z,8,11)>.2?Region.MARE:Region.PLAINS;
  int cells=Math.max(4,(int)Math.round(circumference/1024)),cx=(int)Math.floor(x/circumference*cells),cz=(int)Math.floor(z/circumference*cells);double width=circumference/cells,best=1.45,radius=0,weighted=0,total=0;
  for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++){int gx=cx+dx,gz=cz+dz,wrapped=Math.floorMod(gx,cells);if(random(wrapped,gz,20)>.68)continue;double px=(gx+.15+.7*random(wrapped,gz,21))*width,pz=(gz+.15+.7*random(wrapped,gz,22))*width,r=width*(.20+.25*random(wrapped,gz,23)),t=Math.hypot(x-px,z-pz)/r;if(t<1.45){double weight=1-smooth(1.1,1.45,t);weighted+=(base(px,pz)+relief(t,r))*weight;total+=weight;}if(t<best){best=t;radius=r;}}
  if(radius>0&&total>1e-12){double blend=Math.min(1,total);height=height*(1-blend)+(weighted/total+2*noise(x,z,384,17))*blend;if(best<.78)region=radius>=320&&best<.18?Region.RIM:Region.BASIN;else region=Region.RIM;}
  boolean ice=region==Region.BASIN&&Math.abs(z)>circumference*.205&&noise(x,z,192,29)>-.35;
  return new Sample((int)Math.round(Math.clamp(height,-35,260)),region,ice);
 }
 public static int color(Sample s){return s.ice?0xcbdde4:switch(s.region){case PLAINS->0xaaa8a3;case MARE->0x676972;case RIM->0xc0bcb4;case BASIN->0x8b8884;};}
 public int color(double nx,double ny,double nz){return color(sample(Math.atan2(nx,ny)/(2*Math.PI)*circumference,Math.asin(Math.clamp(nz,-1,1))/(2*Math.PI)*circumference));}
}
