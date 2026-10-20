package local.simple.tvoverlay;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

/** Compact remote-operated submenu matching the switcher's feathered scrim. */
final class HomeMenuDialog extends Dialog {
    private final Context context;
    private View first;
    HomeMenuDialog(Context context,String title,String message,String[] labels,java.util.function.IntConsumer selected,Runnable back){
        super(context);this.context=context;requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root=new LinearLayout(context);root.setOrientation(1);root.setPadding(dp(24),dp(22),dp(24),dp(20));root.setBackground(new MenuBackdropDrawable());
        TextView heading=text(title,16);heading.setTypeface(null,Typeface.BOLD);root.addView(heading);
        if(message!=null&&!message.isEmpty()){TextView description=text(message,12);description.setTextColor(0xffcbd2da);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(8);root.addView(description,p);}
        View divider=new View(context);divider.setBackgroundColor(0x40ffffff);LinearLayout.LayoutParams line=new LinearLayout.LayoutParams(-1,dp(1));line.topMargin=dp(12);line.bottomMargin=dp(6);root.addView(divider,line);
        ScrollView scroll=new ScrollView(context);scroll.setFocusable(false);LinearLayout choices=new LinearLayout(context);choices.setOrientation(1);scroll.addView(choices);
        int maxHeight=Math.max(dp(80),context.getResources().getDisplayMetrics().heightPixels-dp(210));
        root.addView(scroll,new LinearLayout.LayoutParams(-1,Math.min(dp(labels.length*42),maxHeight)));
        View[] rows=new View[labels.length];
        for(int i=0;i<labels.length;i++){
            final int index=i;TextView row=text(labels[i],13);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(10),dp(4),dp(10),dp(4));row.setMaxLines(2);row.setEllipsize(android.text.TextUtils.TruncateAt.END);row.setId(View.generateViewId());row.setFocusable(true);row.setFocusableInTouchMode(true);row.setClickable(true);
            row.setOnFocusChangeListener((v,focused)->v.setBackground(surface(focused)));row.setBackground(surface(false));
            row.setOnClickListener(v->{dismiss();selected.accept(index);});choices.addView(row,new LinearLayout.LayoutParams(-1,dp(42)));rows[i]=row;
        }
        TextView close=text(back==null?"返回 · 关闭":"返回 · 上一级",11);close.setTextColor(0xffcbd2da);close.setGravity(Gravity.CENTER_VERTICAL);close.setPadding(dp(10),0,dp(10),0);close.setFocusable(true);close.setFocusableInTouchMode(true);close.setClickable(true);close.setId(View.generateViewId());close.setOnFocusChangeListener((v,f)->v.setBackground(surface(f)));close.setOnClickListener(v->{dismiss();if(back!=null)back.run();});root.addView(close,new LinearLayout.LayoutParams(-1,dp(34)));
        for(int i=0;i<rows.length;i++){rows[i].setNextFocusUpId(i==0?close.getId():rows[i-1].getId());rows[i].setNextFocusDownId(i==rows.length-1?close.getId():rows[i+1].getId());rows[i].setNextFocusLeftId(rows[i].getId());rows[i].setNextFocusRightId(rows[i].getId());}
        if(rows.length>0){close.setNextFocusUpId(rows[rows.length-1].getId());close.setNextFocusDownId(rows[0].getId());}first=rows.length==0?close:rows[0];
        setContentView(root);setCanceledOnTouchOutside(true);
        setOnKeyListener((d,key,event)->{if(key!=KeyEvent.KEYCODE_BACK)return false;if(event.getAction()==KeyEvent.ACTION_UP){dismiss();if(back!=null)back.run();}return true;});
    }
    @Override public void show(){super.show();Window w=getWindow();w.setBackgroundDrawableResource(android.R.color.transparent);w.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);w.setDimAmount(0);w.setLayout(Math.min(dp(420),context.getResources().getDisplayMetrics().widthPixels-dp(32)),WindowManager.LayoutParams.WRAP_CONTENT);first.requestFocus();}
    private TextView text(String value,int size){TextView v=new TextView(context);v.setText(value);v.setTextColor(Color.WHITE);v.setTextSize(size);return v;}
    private GradientDrawable surface(boolean focused){GradientDrawable d=new GradientDrawable();d.setColor(focused?0x557d8896:Color.TRANSPARENT);d.setCornerRadius(dp(6));return d;}
    private int dp(int value){return Math.round(value*context.getResources().getDisplayMetrics().density);}
}
