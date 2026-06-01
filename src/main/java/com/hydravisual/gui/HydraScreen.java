package com.hydravisual.gui;

import com.hydravisual.HydraVisualClient;
import com.hydravisual.module.Module;
import com.hydravisual.module.ModuleManager;
import com.hydravisual.module.Setting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class HydraScreen extends Screen {

    private enum Tab {
        VISUALS("Visuals"), FRIENDS("Friends"), UTILITIES("Utilities"),
        COSMETICS("Cosmetics"), CONFIGS("Configs");
        final String label;
        Tab(String l) { label = l; }
    }

    private Tab selectedTab = Tab.VISUALS;
    private final ModuleManager moduleManager;
    private int px, py, pw, ph;
    private static final int SIDEBAR_W = 110, TAB_H = 24, CORNER_R = 8;

    private float openAnim = 0f, scrollOffset = 0f, scrollTarget = 0f, tabIndicatorY = -1f;
    private float[] tabHoverAnim = new float[Tab.values().length];

    // Bind mode: middle-click a module, then press a key
    private int bindingModuleIndex = -1;

    // Settings panel: right-click a module
    private int settingsModuleIndex = -1;
    private int draggingSlider = -1; // which setting slider is being dragged

    public HydraScreen() {
        super(Text.literal("Menu"));
        this.moduleManager = HydraVisualClient.INSTANCE.getModuleManager();
    }

    @Override
    protected void init() {
        pw = Math.min(480, width - 40); ph = Math.min(340, height - 40);
        px = (width - pw) / 2; py = (height - ph) / 2;
        openAnim = 0f; tabIndicatorY = -1f; scrollOffset = 0f; scrollTarget = 0f;
        bindingModuleIndex = -1; settingsModuleIndex = -1; draggingSlider = -1;
    }

    // ===== COLOR HELPERS =====
    private static int hsbToRgb(float h, float s, float b) {
        h = (h - (float)Math.floor(h)) * 6f;
        float f = h - (float)Math.floor(h), p = b*(1-s), q = b*(1-s*f), t = b*(1-s*(1-f));
        int r,g,bl;
        switch((int)h) {
            case 0->{r=(int)(b*255+.5f);g=(int)(t*255+.5f);bl=(int)(p*255+.5f);}
            case 1->{r=(int)(q*255+.5f);g=(int)(b*255+.5f);bl=(int)(p*255+.5f);}
            case 2->{r=(int)(p*255+.5f);g=(int)(b*255+.5f);bl=(int)(t*255+.5f);}
            case 3->{r=(int)(p*255+.5f);g=(int)(q*255+.5f);bl=(int)(b*255+.5f);}
            case 4->{r=(int)(t*255+.5f);g=(int)(p*255+.5f);bl=(int)(b*255+.5f);}
            default->{r=(int)(b*255+.5f);g=(int)(p*255+.5f);bl=(int)(q*255+.5f);}
        }
        return (r<<16)|(g<<8)|bl;
    }
    private int accent(int off) { float h=((System.currentTimeMillis()+off)%5000)/5000f; return 0xFF000000|hsbToRgb(h,0.5f,0.9f); }
    private static int withAlpha(int c,int a) { return (Math.max(0,Math.min(255,a))<<24)|(c&0xFFFFFF); }
    private static int lerp(int a,int b,float t) {
        t=Math.max(0,Math.min(1,t));
        return ((int)(((a>>24)&0xFF)+(((b>>24)&0xFF)-((a>>24)&0xFF))*t)<<24)|
               ((int)(((a>>16)&0xFF)+(((b>>16)&0xFF)-((a>>16)&0xFF))*t)<<16)|
               ((int)(((a>>8)&0xFF)+(((b>>8)&0xFF)-((a>>8)&0xFF))*t)<<8)|
               (int)((a&0xFF)+((b&0xFF)-(a&0xFF))*t);
    }

    // ===== ROUNDED RECT =====
    private void fillR(DrawContext ctx,int x,int y,int w,int h,int r,int color) {
        if(((color>>24)&0xFF)==0) return;
        r=Math.min(r,Math.min(w/2,h/2));
        ctx.fill(x+r,y,x+w-r,y+h,color);
        ctx.fill(x,y+r,x+r,y+h-r,color);
        ctx.fill(x+w-r,y+r,x+w,y+h-r,color);
        for(int cy2=0;cy2<r;cy2++) for(int cx2=0;cx2<r;cx2++) {
            float d=(float)Math.sqrt((r-cx2-.5f)*(r-cx2-.5f)+(r-cy2-.5f)*(r-cy2-.5f));
            if(d<=r) {
                ctx.fill(x+cx2,y+cy2,x+cx2+1,y+cy2+1,color);
                ctx.fill(x+w-cx2-1,y+cy2,x+w-cx2,y+cy2+1,color);
                ctx.fill(x+cx2,y+h-cy2-1,x+cx2+1,y+h-cy2,color);
                ctx.fill(x+w-cx2-1,y+h-cy2-1,x+w-cx2,y+h-cy2,color);
            }
        }
    }
    private void sidebarBg(DrawContext ctx,int x,int y,int w,int h,int r,int color) {
        r=Math.min(r,Math.min(w/2,h/2));
        ctx.fill(x+r,y,x+w,y+h,color);
        ctx.fill(x,y+r,x+r,y+h-r,color);
        for(int cy2=0;cy2<r;cy2++) for(int cx2=0;cx2<r;cx2++) {
            float d=(float)Math.sqrt((r-cx2-.5f)*(r-cx2-.5f)+(r-cy2-.5f)*(r-cy2-.5f));
            if(d<=r) { ctx.fill(x+cx2,y+cy2,x+cx2+1,y+cy2+1,color); ctx.fill(x+cx2,y+h-cy2-1,x+cx2+1,y+h-cy2,color); }
        }
    }

    // ===== KEY NAME =====
    private String keyName(int code) {
        if(code<=0) return "";
        String n = GLFW.glfwGetKeyName(code, GLFW.glfwGetKeyScancode(code));
        if(n!=null) return n.toUpperCase();
        return switch(code) {
            case GLFW.GLFW_KEY_F1->"F1"; case GLFW.GLFW_KEY_F2->"F2"; case GLFW.GLFW_KEY_F3->"F3";
            case GLFW.GLFW_KEY_F4->"F4"; case GLFW.GLFW_KEY_F5->"F5"; case GLFW.GLFW_KEY_F6->"F6";
            case GLFW.GLFW_KEY_F7->"F7"; case GLFW.GLFW_KEY_F8->"F8"; case GLFW.GLFW_KEY_F9->"F9";
            case GLFW.GLFW_KEY_F10->"F10"; case GLFW.GLFW_KEY_F11->"F11"; case GLFW.GLFW_KEY_F12->"F12";
            case GLFW.GLFW_KEY_LEFT_SHIFT->"LSH"; case GLFW.GLFW_KEY_RIGHT_SHIFT->"RSH";
            case GLFW.GLFW_KEY_LEFT_CONTROL->"LCT"; case GLFW.GLFW_KEY_RIGHT_CONTROL->"RCT";
            case GLFW.GLFW_KEY_LEFT_ALT->"LALT"; case GLFW.GLFW_KEY_RIGHT_ALT->"RALT";
            case GLFW.GLFW_KEY_INSERT->"INS"; case GLFW.GLFW_KEY_HOME->"HOM";
            case GLFW.GLFW_KEY_PAGE_UP->"PGU"; case GLFW.GLFW_KEY_PAGE_DOWN->"PGD";
            case GLFW.GLFW_KEY_END->"END"; case GLFW.GLFW_KEY_CAPS_LOCK->"CAP";
            case GLFW.GLFW_KEY_TAB->"TAB"; case GLFW.GLFW_KEY_SPACE->"SPC";
            default -> "K"+code;
        };
    }

    // ===== RENDER =====
    @Override
    public void render(DrawContext ctx,int mx,int my,float delta) {
        openAnim=Math.min(1f,openAnim+delta*0.07f);
        float ease=1f-(1f-openAnim)*(1f-openAnim)*(1f-openAnim);
        if(ease<0.01f) return;
        int alpha=(int)(ease*255);
        scrollOffset+=(scrollTarget-scrollOffset)*Math.min(1f,delta*8f);

        ctx.fill(0,0,width,height,withAlpha(0xFF000000,(int)(ease*100)));
        for(int i=5;i>=1;i--) fillR(ctx,px-i,py-i,pw+i*2,ph+i*2,CORNER_R+i,withAlpha(0xFF000000,(int)(ease*(8-i))));
        fillR(ctx,px,py,pw,ph,CORNER_R,withAlpha(0xFF141418,alpha));
        sidebarBg(ctx,px,py,SIDEBAR_W,ph,CORNER_R,withAlpha(0xFF1a1a1f,alpha));
        ctx.fill(px+SIDEBAR_W,py+10,px+SIDEBAR_W+1,py+ph-10,withAlpha(0xFF2a2a32,alpha));

        drawSidebar(ctx,mx,my,alpha,delta);

        // If settings panel is open, draw it; otherwise draw content
        if(settingsModuleIndex>=0) drawSettingsPanel(ctx,mx,my,alpha);
        else drawContent(ctx,mx,my,alpha,delta);
    }

    // ===== SIDEBAR =====
    private void drawSidebar(DrawContext ctx,int mx,int my,int alpha,float delta) {
        int logoY=py+14;
        ctx.drawText(textRenderer,"SeladalaVisual",px+14,logoY,withAlpha(0xFFe0e0e8,alpha),false);
        ctx.drawText(textRenderer,"v1.3.0",px+14,logoY+12,withAlpha(0xFF505058,alpha),false);
        int sepY=logoY+28;
        ctx.fill(px+12,sepY,px+SIDEBAR_W-12,sepY+1,withAlpha(0xFF2a2a32,alpha));
        int tabStartY=sepY+10;
        float targetY=tabStartY;

        for(int i=0;i<Tab.values().length;i++) {
            Tab tab=Tab.values()[i]; int tabY=tabStartY+i*TAB_H;
            boolean sel=tab==selectedTab, hov=mx>=px+4&&mx<px+SIDEBAR_W-4&&my>=tabY&&my<tabY+TAB_H;
            if(sel) targetY=tabY;
            float ht=(sel||hov)?1f:0f;
            tabHoverAnim[i]+=(ht-tabHoverAnim[i])*Math.min(1f,delta*10f);
            if(tabHoverAnim[i]>0.01f) fillR(ctx,px+6,tabY+1,SIDEBAR_W-12,TAB_H-2,4,withAlpha(0xFFFFFFFF,(int)(alpha*0.06f*tabHoverAnim[i])));
            int lc=sel?withAlpha(0xFFf0f0f4,alpha):lerp(withAlpha(0xFF707078,alpha),withAlpha(0xFFb0b0b8,alpha),tabHoverAnim[i]);
            ctx.drawText(textRenderer,tab.label,px+18,tabY+(TAB_H-8)/2,lc,false);
        }

        if(tabIndicatorY<0) tabIndicatorY=targetY; else tabIndicatorY+=(targetY-tabIndicatorY)*Math.min(1f,delta*7f);
        int iy=(int)tabIndicatorY+TAB_H/2-2;
        int ic=accent(0);
        ctx.fill(px+8,iy,px+11,iy+5,withAlpha(ic,alpha));

        ctx.drawText(textRenderer,"RShift",px+14,py+ph-18,withAlpha(0xFF383840,alpha),false);
    }

    // ===== CONTENT =====
    private void drawContent(DrawContext ctx,int mx,int my,int alpha,float delta) {
        int cx=px+SIDEBAR_W+12, cy=py+14, cw=pw-SIDEBAR_W-24;
        ctx.drawText(textRenderer,selectedTab.label,cx,cy,withAlpha(0xFFe8e8f0,alpha),false);
        int top=cy+18, area=ph-28-18;

        switch(selectedTab) {
            case VISUALS -> drawModuleGrid(ctx,cx,top,cw,area,mx,my,alpha,delta);
            default -> drawPlaceholder(ctx,cx,top,cw,area,alpha,selectedTab.label);
        }
    }

    // ===== MODULE GRID =====
    private void drawModuleGrid(DrawContext ctx,int x,int y,int w,int maxH,int mx,int my,int alpha,float delta) {
        List<Module> modules=moduleManager.getModules();
        int gap=4, colW=(w-gap)/2, cardH=44;
        int rows=(modules.size()+1)/2;
        int totalH=rows*(cardH+gap)-gap;
        int maxScroll=Math.max(0,totalH-maxH);
        scrollTarget=Math.max(0,Math.min(scrollTarget,maxScroll));

        for(int i=0;i<modules.size();i++) {
            Module mod=modules.get(i);
            int col=i%2, row=i/2;
            int cx=x+col*(colW+gap), cy=y+row*(cardH+gap)-(int)scrollOffset;
            if(cy+cardH<y||cy>y+maxH) continue;

            boolean hov=mx>=cx&&mx<=cx+colW&&my>=Math.max(y,cy)&&my<Math.min(y+maxH,cy+cardH);
            boolean on=mod.isEnabled();
            int bg=on?(hov?withAlpha(0xFF242430,alpha):withAlpha(0xFF1e1e28,alpha)):(hov?withAlpha(0xFF1e1e24,alpha):withAlpha(0xFF18181e,alpha));
            fillR(ctx,cx,cy,colW,cardH,5,bg);

            if(on) { int rc=accent(i*200); ctx.fill(cx,cy+3,cx+2,cy+cardH-3,withAlpha(rc,alpha)); }

            // Name
            int nc=on?withAlpha(0xFFe8e8f0,alpha):withAlpha(0xFF808088,alpha);
            ctx.drawText(textRenderer,mod.getName(),cx+8,cy+6,nc,on);

            // Keybind badge
            int bind=mod.getKeyBind();
            boolean isBinding=bindingModuleIndex==i;
            String bindText=isBinding?"[...]":(bind>0?"["+keyName(bind)+"]":"");
            if(!bindText.isEmpty()) {
                int bw=textRenderer.getWidth(bindText);
                int bx=cx+colW-bw-6;
                ctx.drawText(textRenderer,bindText,bx,cy+6,withAlpha(isBinding?0xFFffcc44:0xFF606070,alpha),false);
            }

            // Description
            String desc=mod.getDescription();
            if(textRenderer.getWidth(desc)>colW-16) { while(textRenderer.getWidth(desc+"..")>colW-16&&desc.length()>3) desc=desc.substring(0,desc.length()-1); desc+=".."; }
            ctx.drawText(textRenderer,desc,cx+8,cy+20,withAlpha(0xFF505058,alpha),false);

            // Settings icon (if has settings) + enabled dot
            if(mod.hasSettings()) {
                ctx.drawText(textRenderer,"\u2699",cx+colW-12,cy+cardH-14,withAlpha(0xFF505060,alpha),false);
            }
            if(on) {
                int dc=accent(i*200); ctx.fill(cx+colW-10,cy+6,cx+colW-6,cy+10,withAlpha(dc,alpha));
            }

            // Hint at bottom: SCM=bind, PKM=settings
            if(hov) {
                String hint="СКМ-бинд";
                if(mod.hasSettings()) hint+=" ПКМ-настр.";
                ctx.drawText(textRenderer,hint,cx+8,cy+cardH-12,withAlpha(0xFF404050,alpha/2),false);
            }
        }

        if(totalH>maxH&&maxScroll>0) {
            int bx=x+w-2; float r=(float)maxH/totalH;
            int th=Math.max(12,(int)(maxH*r)), ty=y+(int)((scrollOffset/maxScroll)*(maxH-th));
            ctx.fill(bx,y,bx+2,y+maxH,withAlpha(0xFF1a1a20,alpha/3));
            fillR(ctx,bx,ty,2,th,1,withAlpha(0xFF404048,alpha));
        }
    }

    // ===== SETTINGS PANEL =====
    private void drawSettingsPanel(DrawContext ctx,int mx,int my,int alpha) {
        List<Module> modules=moduleManager.getModules();
        if(settingsModuleIndex<0||settingsModuleIndex>=modules.size()) { settingsModuleIndex=-1; return; }
        Module mod=modules.get(settingsModuleIndex);
        List<Setting> settings=mod.getSettings();

        int cx=px+SIDEBAR_W+12, cy=py+14, cw=pw-SIDEBAR_W-24;

        // Header: back arrow + module name
        ctx.drawText(textRenderer,"\u2190 "+mod.getName()+" — Настройки",cx,cy,withAlpha(0xFFe8e8f0,alpha),false);

        int sy=cy+22;
        for(int i=0;i<settings.size();i++) {
            Setting s=settings.get(i);
            if(s.getType()==Setting.Type.SLIDER) {
                // Label + value
                String label=s.getName()+": "+String.format("%.1f",s.getValue());
                ctx.drawText(textRenderer,label,cx+4,sy,withAlpha(0xFFc0c0c8,alpha),false);
                sy+=14;

                // Slider track
                int sw=cw-16, sh=8, sx2=cx+4;
                fillR(ctx,sx2,sy,sw,sh,3,withAlpha(0xFF1a1a24,alpha));

                // Filled portion
                float pct=(float)((s.getValue()-s.getMin())/(s.getMax()-s.getMin()));
                int filled=(int)(sw*pct);
                if(filled>0) fillR(ctx,sx2,sy,filled,sh,3,withAlpha(accent(i*200),alpha));

                // Knob
                int kx=sx2+filled-4;
                fillR(ctx,kx,sy-2,8,sh+4,4,withAlpha(0xFFe0e0e8,alpha));

                sy+=sh+10;
            } else if(s.getType()==Setting.Type.TOGGLE) {
                // Toggle row
                boolean on=s.isEnabled();
                int tw=16, th=10;
                int tx=cx+cw-tw-8, ty2=sy+1;

                // Label
                ctx.drawText(textRenderer,s.getName(),cx+4,sy,withAlpha(on?0xFFe0e0e8:0xFF808088,alpha),false);

                // Toggle bg
                fillR(ctx,tx,ty2,tw,th,4,withAlpha(on?accent(i*200):0xFF1a1a24,alpha));
                // Knob
                int knobX=on?tx+tw-th:tx;
                fillR(ctx,knobX,ty2,th,th,4,withAlpha(0xFFe0e0e8,alpha));

                sy+=th+10;
            } else if(s.getType()==Setting.Type.ENUM) {
                // Enum row: label    < option >
                ctx.drawText(textRenderer,s.getName(),cx+4,sy,withAlpha(0xFFc0c0c8,alpha),false);

                // Draw selector: [< selected >]
                String sel="< "+s.getSelected()+" >";
                int sw2=textRenderer.getWidth(sel);
                int selX=cx+cw-sw2-8;
                fillR(ctx,selX-4,sy-2,sw2+8,12,3,withAlpha(0xFF1a1a28,alpha));
                ctx.drawText(textRenderer,sel,selX,sy,withAlpha(accent(i*200),alpha),false);

                sy+=16;
            } else if(s.getType()==Setting.Type.TOGGLE_ROW) {
                // Label
                ctx.drawText(textRenderer,s.getName()+":",cx+4,sy,withAlpha(0xFFc0c0c8,alpha),false);
                sy+=14;
                // Draw each toggle button in a row
                String[] labels=s.getRowLabels();
                boolean[] states=s.getRowStates();
                int btnX=cx+4;
                for(int j=0;j<labels.length;j++) {
                    int bw=textRenderer.getWidth(labels[j])+12;
                    boolean on=states[j];
                    fillR(ctx,btnX,sy,bw,14,4,withAlpha(on?accent(i*200+j*100):0xFF1a1a24,alpha));
                    ctx.drawText(textRenderer,labels[j],btnX+6,sy+3,withAlpha(on?0xFFf0f0f4:0xFF606068,alpha),false);
                    btnX+=bw+4;
                }
                sy+=20;
            }
        }

        // Close hint
        sy+=10;
        ctx.drawText(textRenderer,"ЛКМ по стрелке или ESC — назад",cx+4,sy,withAlpha(0xFF404050,alpha/2),false);
    }

    // ===== PLACEHOLDER =====
    private void drawPlaceholder(DrawContext ctx,int x,int y,int w,int h,int alpha,String title) {
        int cx2=x+w/2, cy2=y+h/2-10;
        int tw=textRenderer.getWidth(title);
        ctx.drawText(textRenderer,title,cx2-tw/2,cy2,withAlpha(0xFF606068,alpha),false);
        int sw2=textRenderer.getWidth("Coming soon");
        ctx.drawText(textRenderer,"Coming soon",cx2-sw2/2,cy2+14,withAlpha(0xFF383840,alpha),false);
    }

    // ===== INPUT =====
    @Override
    public boolean mouseClicked(double mx,double my,int button) {
        // Settings panel — back button or toggle clicks
        if(settingsModuleIndex>=0) {
            int cx=px+SIDEBAR_W+12, cy=py+14, cw=pw-SIDEBAR_W-24;

            // Back arrow click
            if(mx>=cx&&mx<=cx+80&&my>=cy&&my<=cy+12) { settingsModuleIndex=-1; draggingSlider=-1; return true; }

            // Settings interactions
            List<Module> modules=moduleManager.getModules();
            if(settingsModuleIndex<modules.size()) {
                Module mod=modules.get(settingsModuleIndex);
                List<Setting> settings=mod.getSettings();
                int sy=cy+22;
                for(int i=0;i<settings.size();i++) {
                    Setting s=settings.get(i);
                    if(s.getType()==Setting.Type.SLIDER) {
                        sy+=14;
                        int sw2=cw-16, sh=8, sx2=cx+4;
                        if(mx>=sx2&&mx<=sx2+sw2&&my>=sy-4&&my<=sy+sh+4) {
                            draggingSlider=i;
                            updateSlider(s,mx,sx2,sw2);
                            return true;
                        }
                        sy+=sh+10;
                    } else if(s.getType()==Setting.Type.TOGGLE) {
                        int tw=16, th=10, tx=cx+cw-tw-8, ty2=sy+1;
                        if(mx>=tx&&mx<=tx+tw&&my>=ty2&&my<=ty2+th) { s.toggle(); return true; }
                        // Also click on label toggles
                        if(mx>=cx&&mx<=cx+cw&&my>=sy&&my<=sy+th+2) { s.toggle(); return true; }
                        sy+=th+10;
                    } else if(s.getType()==Setting.Type.ENUM) {
                        // Click anywhere on row = next; right-click = prev
                        if(mx>=cx&&mx<=cx+cw&&my>=sy-2&&my<=sy+14) {
                            if(button==1) s.prevOption(); else s.nextOption();
                            return true;
                        }
                        sy+=16;
                    } else if(s.getType()==Setting.Type.TOGGLE_ROW) {
                        sy+=14; // skip label row
                        String[] labels=s.getRowLabels();
                        int btnX2=cx+4;
                        for(int j=0;j<labels.length;j++) {
                            int bw=textRenderer.getWidth(labels[j])+12;
                            if(mx>=btnX2&&mx<=btnX2+bw&&my>=sy&&my<=sy+14) {
                                s.toggleRow(j);
                                return true;
                            }
                            btnX2+=bw+4;
                        }
                        sy+=20;
                    }
                }
            }
            return true;
        }

        // Tab clicks
        int sepY=py+14+28, tabStartY=sepY+10;
        for(int i=0;i<Tab.values().length;i++) {
            int tabY=tabStartY+i*TAB_H;
            if(mx>=px+4&&mx<px+SIDEBAR_W-4&&my>=tabY&&my<tabY+TAB_H) {
                selectedTab=Tab.values()[i]; scrollTarget=0; scrollOffset=0; return true;
            }
        }

        // Module grid clicks
        if(selectedTab==Tab.VISUALS) {
            int cx=px+SIDEBAR_W+12, cy=py+14+18, cw=pw-SIDEBAR_W-24;
            int area=ph-28-18, gap=4, colW=(cw-gap)/2, cardH=44;
            List<Module> modules=moduleManager.getModules();

            for(int i=0;i<modules.size();i++) {
                int col=i%2, row=i/2;
                int cardX=cx+col*(colW+gap), cardY=cy+row*(cardH+gap)-(int)scrollOffset;
                if(cardY+cardH<cy||cardY>cy+area) continue;
                if(mx>=cardX&&mx<=cardX+colW&&my>=Math.max(cy,cardY)&&my<Math.min(cy+area,cardY+cardH)) {
                    if(button==0) { modules.get(i).toggle(); return true; } // LMB = toggle
                    if(button==2) { // Middle = bind mode
                        bindingModuleIndex=i; return true;
                    }
                    if(button==1&&modules.get(i).hasSettings()) { // RMB = settings
                        settingsModuleIndex=i; return true;
                    }
                }
            }
        }
        return super.mouseClicked(mx,my,button);
    }

    @Override
    public boolean mouseDragged(double mx,double my,int button,double dx,double dy) {
        if(settingsModuleIndex>=0&&draggingSlider>=0) {
            List<Module> modules=moduleManager.getModules();
            if(settingsModuleIndex<modules.size()) {
                Module mod=modules.get(settingsModuleIndex);
                List<Setting> settings=mod.getSettings();
                if(draggingSlider<settings.size()) {
                    Setting s=settings.get(draggingSlider);
                    int cx=px+SIDEBAR_W+12, cw=pw-SIDEBAR_W-24;
                    int sw2=cw-16, sx2=cx+4;
                    updateSlider(s,mx,sx2,sw2);
                }
            }
            return true;
        }
        return super.mouseDragged(mx,my,button,dx,dy);
    }

    @Override
    public boolean mouseReleased(double mx,double my,int button) {
        draggingSlider=-1;
        return super.mouseReleased(mx,my,button);
    }

    private void updateSlider(Setting s,double mx,int sx,int sw) {
        float pct=(float)(mx-sx)/sw;
        pct=Math.max(0,Math.min(1,pct));
        s.setValue(s.getMin()+(s.getMax()-s.getMin())*pct);
    }

    @Override
    public boolean mouseScrolled(double mx,double my,double hA,double vA) {
        if(settingsModuleIndex<0) { scrollTarget-=(float)(vA*30); scrollTarget=Math.max(0,scrollTarget); }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode,int scanCode,int modifiers) {
        // Binding mode — assign key
        if(bindingModuleIndex>=0) {
            List<Module> modules=moduleManager.getModules();
            if(bindingModuleIndex<modules.size()) {
                if(keyCode==GLFW.GLFW_KEY_ESCAPE||keyCode==GLFW.GLFW_KEY_DELETE) {
                    modules.get(bindingModuleIndex).setKeyBind(-1); // Remove bind
                } else {
                    modules.get(bindingModuleIndex).setKeyBind(keyCode); // Set bind
                }
            }
            bindingModuleIndex=-1;
            return true;
        }

        // Close settings with ESC
        if(settingsModuleIndex>=0&&keyCode==GLFW.GLFW_KEY_ESCAPE) { settingsModuleIndex=-1; return true; }

        if(keyCode==GLFW.GLFW_KEY_RIGHT_SHIFT) { close(); return true; }
        if(keyCode==GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        return super.keyPressed(keyCode,scanCode,modifiers);
    }

    @Override public boolean shouldPause() { return false; }
}
