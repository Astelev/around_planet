package io.github.astelev.around_planet.propulsion;

import java.io.*;
import java.util.*;

/** Positions of the actual OBJ inner material; independent of exporter normals and vertex numbering. */
public record NozzleLining(List<Face> faces,float bottom,float top,float mouthRadius) {
    public record Point(float x,float y,float z) {}
    public record Face(List<Point> points,Point normal) {}
    public static final NozzleLining EMPTY=new NozzleLining(List.of(),-1.125f,.0625f,.36f);
    public float outlet(){return bottom-.005f;}
    public float plumeScale(){return mouthRadius/.24f;}
    public static NozzleLining read(Reader source)throws IOException {
        var vertices=new ArrayList<Point>();var faces=new ArrayList<Face>();String material="";
        try(var reader=new BufferedReader(source)){
            String line;
            while((line=reader.readLine())!=null){
                line=line.split("#",2)[0].trim();if(line.isEmpty())continue;
                var words=line.split("\\s+");
                if(words[0].equals("v"))vertices.add(new Point(Float.parseFloat(words[1])-.5f,Float.parseFloat(words[2])-.5f,Float.parseFloat(words[3])-.5f));
                else if(words[0].equals("usemtl"))material=words[1];
                else if(words[0].equals("f")&&material.equals("inner")){
                    var points=new ArrayList<Point>();
                    for(int i=1;i<words.length;i++){int index=Integer.parseInt(words[i].split("/",2)[0]);points.add(vertices.get(index<0?vertices.size()+index:index-1));}
                    if(points.size()<3)throw new IOException("Nozzle face has fewer than three vertices");
                    if(points.size()==4)add(faces,points);
                    else for(int i=1;i<points.size()-1;i++)add(faces,List.of(points.get(0),points.get(i),points.get(i+1),points.get(i+1)));
                }
            }
        }catch(IllegalArgumentException|IndexOutOfBoundsException e){throw new IOException("Invalid nozzle OBJ",e);}
        if(faces.isEmpty())throw new IOException("No nozzle faces use material 'inner'");
        float bottom=Float.POSITIVE_INFINITY,top=Float.NEGATIVE_INFINITY,radius=0;
        for(var face:faces)for(var p:face.points){bottom=Math.min(bottom,p.y);top=Math.max(top,p.y);}
        for(var face:faces)for(var p:face.points)if(Math.abs(p.y-bottom)<1e-5)radius=Math.max(radius,Math.max(Math.abs(p.x),Math.abs(p.z)));
        return new NozzleLining(List.copyOf(faces),bottom,top,radius);
    }
    private static void add(List<Face> faces,List<Point> points)throws IOException {
        var a=points.get(0);var b=points.get(1);var c=points.get(2);
        float ax=b.x-a.x,ay=b.y-a.y,az=b.z-a.z,bx=c.x-a.x,by=c.y-a.y,bz=c.z-a.z;
        float nx=ay*bz-az*by,ny=az*bx-ax*bz,nz=ax*by-ay*bx,length=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);
        if(!Float.isFinite(length)||length<1e-10)throw new IOException("Degenerate nozzle face");
        faces.add(new Face(List.copyOf(points),new Point(nx/length,ny/length,nz/length)));
    }
}
