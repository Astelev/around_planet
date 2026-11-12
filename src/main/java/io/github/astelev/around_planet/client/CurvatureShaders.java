package io.github.astelev.around_planet.client;
import java.util.*;
import org.lwjgl.opengl.GL20;
/** Same camera-relative parabola for stock/Minecraft/Sodium and DH; no vertex data is rewritten. */
public final class CurvatureShaders {
    private static final Map<Integer,int[]> UNIFORMS=new java.util.concurrent.ConcurrentHashMap<>();
    private static final float[] VP=new float[16],IVP=new float[16];
    public static void clear(){UNIFORMS.clear();}
    public static boolean worldProgram(String name){String path=name.substring(name.indexOf(':')+1);return (path.startsWith("rendertype_") && !path.contains("gui")) || path.equals("position_color") || path.equals("particle");}
    public static String patch(String source){
        if(source==null || source.contains("apCurveView"))return source;
        var pattern=java.util.regex.Pattern.compile("gl_Position\\s*=\\s*(ProjMat|u_ProjectionMatrix)\\s*\\*\\s*([^;]+);");
        var matcher=pattern.matcher(source);var changed=new StringBuffer();boolean found=false;
        while(matcher.find()){found=true;matcher.appendReplacement(changed,java.util.regex.Matcher.quoteReplacement("gl_Position = "+matcher.group(1)+" * apCurveView("+matcher.group(2)+");"));}matcher.appendTail(changed);if(!found)return source;
        String helper="uniform float apCurveRadius;\nuniform mat4 apView;\nuniform mat4 apInverseView;\nvec4 apCurveView(vec4 v){if(apCurveRadius>0.0){vec4 p=apInverseView*v;p.y-=dot(p.xz,p.xz)/(2.0*apCurveRadius);return apView*p;}return v;}\n";
        return changed.toString().replaceFirst("void\\s+main\\s*\\(\\s*(?:void)?\\s*\\)",java.util.regex.Matcher.quoteReplacement(helper+"void main()"));
    }
    public static void upload(int program){
        if(UNIFORMS.size()>256)UNIFORMS.clear();var u=UNIFORMS.computeIfAbsent(program,p->new int[]{GL20.glGetUniformLocation(p,"apCurveRadius"),GL20.glGetUniformLocation(p,"apView"),GL20.glGetUniformLocation(p,"apInverseView")});
        if(u[0]<0)return;float radius=(float)SurfaceRenderState.radius();GL20.glUniform1f(u[0],radius);
        if(radius>0){SurfaceRenderState.view.get(VP);SurfaceRenderState.inverseView.get(IVP);GL20.glUniformMatrix4fv(u[1],false,VP);GL20.glUniformMatrix4fv(u[2],false,IVP);SurfaceRenderState.curvedDraws++;}
    }
    private CurvatureShaders(){}
}
