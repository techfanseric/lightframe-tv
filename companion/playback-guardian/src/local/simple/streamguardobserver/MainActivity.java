package local.simple.streamguardobserver;
import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
public final class MainActivity extends Activity {
 @Override public void onCreate(Bundle state){super.onCreate(state);LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setGravity(Gravity.CENTER);root.setPadding(48,48,48,48);TextView text=new TextView(this);text.setText("轻幕播放守护\n\n保留轻幕首页。从轻幕选择 Netflix 时，先检查连接再打开。\n仅关闭 Netflix 内精确匹配的 Google 服务启用提示，不修改登录信息或 Google 服务状态。\n视频应用前台持续失去 VPN 时，有限次数请求恢复连接。\n\n可在系统无障碍设置中随时关闭守护。");text.setTextSize(21);root.addView(text);setContentView(root);}
}
