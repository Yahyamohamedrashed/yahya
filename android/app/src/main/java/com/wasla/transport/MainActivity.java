package com.wasla.transport;
import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.print.*;
import android.util.Base64;
import android.view.View;
import android.webkit.*;
import android.widget.*;
import java.io.OutputStream;

public class MainActivity extends Activity {
    private WebView web;
    private String origin;
    private ValueCallback<Uri[]> fileCallback;
    private byte[] pendingDocument;
    private static final int PICK_FILE=101, SAVE_FILE=102;
    @Override public void onCreate(Bundle state){super.onCreate(state);showSetup();}
    private int dp(int value){return (int)(getResources().getDisplayMetrics().density*value);}
    private void showSetup(){
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);panel.setPadding(dp(28),dp(70),dp(28),dp(25));panel.setBackgroundColor(Color.rgb(247,249,248));
        TextView title=new TextView(this);title.setText("وصلة");title.setTextSize(36);title.setTextColor(Color.rgb(22,134,115));panel.addView(title);
        TextView intro=new TextView(this);intro.setText("نقل الموظفين\nأدخل رابط خادم وصلة المنشور، ثم سجّل الدخول بحسابك.");intro.setTextSize(16);intro.setPadding(0,dp(15),0,dp(25));panel.addView(intro);
        EditText address=new EditText(this);address.setSingleLine(true);address.setTextDirection(View.TEXT_DIRECTION_LTR);address.setInputType(17);address.setHint("https://your-wasla-server.example");address.setText(getPreferences(MODE_PRIVATE).getString("server",""));panel.addView(address);
        Button connect=new Button(this);connect.setText("الاتصال بالخادم");connect.setOnClickListener(v->{try{Uri uri=Uri.parse(address.getText().toString().trim());if(!"https".equals(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null)throw new IllegalArgumentException();origin=new Uri.Builder().scheme("https").encodedAuthority(uri.getEncodedAuthority()).build().toString();getPreferences(MODE_PRIVATE).edit().putString("server",origin).apply();openServer();}catch(Exception ex){Toast.makeText(this,"أدخل رابط HTTPS صحيحًا",Toast.LENGTH_LONG).show();}});panel.addView(connect);
        TextView note=new TextView(this);note.setText("لا تُحفظ كلمات المرور في التطبيق. تسجيل الدخول يحتاج خادمًا منشورًا ومجهزًا بالحسابات.");note.setTextSize(12);note.setPadding(0,dp(24),0,0);panel.addView(note);ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(panel);setContentView(scroll);
    }
    private boolean trusted(String url){try{Uri u=Uri.parse(url),e=Uri.parse(origin);return "https".equals(u.getScheme())&&e.getHost().equalsIgnoreCase(u.getHost())&&u.getPort()==e.getPort();}catch(Exception ex){return false;}}
    private void openServer(){
        web=new WebView(this);web.setFitsSystemWindows(true);WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(false);s.setAllowContentAccess(false);s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);s.setJavaScriptCanOpenWindowsAutomatically(false);s.setSupportMultipleWindows(false);s.setGeolocationEnabled(false);s.setSafeBrowsingEnabled(true);
        CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);web.addJavascriptInterface(new Documents(),"WaslaAndroid");
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){if(trusted(request.getUrl().toString()))return false;if("https".equals(request.getUrl().getScheme()))try{startActivity(new Intent(Intent.ACTION_VIEW,request.getUrl()));}catch(Exception ignored){}return true;}
            @Override public void onReceivedSslError(WebView view,SslErrorHandler handler,SslError error){handler.cancel();Toast.makeText(MainActivity.this,"شهادة اتصال الخادم غير صالحة",Toast.LENGTH_LONG).show();}
            @Override public void onReceivedError(WebView view,WebResourceRequest request,WebResourceError error){if(request.isForMainFrame())new AlertDialog.Builder(MainActivity.this).setTitle("تعذر الاتصال بالخادم").setMessage("تحقق من نشر الخادم وصحة الرابط واتصال الإنترنت.").setPositiveButton("إعادة المحاولة",(d,w)->web.reload()).setNegativeButton("إعداد الخادم",(d,w)->showSetup()).show();}
        });
        web.setWebChromeClient(new WebChromeClient(){@Override public boolean onShowFileChooser(WebView view,ValueCallback<Uri[]> callback,FileChooserParams params){if(fileCallback!=null)fileCallback.onReceiveValue(null);fileCallback=callback;Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"text/csv","text/plain","application/vnd.ms-excel"});try{startActivityForResult(i,PICK_FILE);return true;}catch(Exception e){fileCallback.onReceiveValue(null);fileCallback=null;return false;}}});
        setContentView(web);web.loadUrl(origin+"/");
    }
    public class Documents {
        @JavascriptInterface public void saveCsv(String name,String data){if(data==null||data.length()>6000000)return;runOnUiThread(()->{if(web==null||!trusted(web.getUrl()))return;try{pendingDocument=Base64.decode(data,Base64.DEFAULT);Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("text/csv");i.putExtra(Intent.EXTRA_TITLE,name.replaceAll("[^a-zA-Z0-9._-]","_")+".csv");startActivityForResult(i,SAVE_FILE);}catch(Exception e){pendingDocument=null;Toast.makeText(MainActivity.this,"تعذر حفظ الملف",Toast.LENGTH_LONG).show();}});}
        @JavascriptInterface public void printPage(){runOnUiThread(()->{if(web==null||!trusted(web.getUrl()))return;PrintManager manager=(PrintManager)getSystemService(PRINT_SERVICE);manager.print("Wasla invoice",web.createPrintDocumentAdapter("Wasla invoice"),new PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build());});}
    }
    @Override protected void onActivityResult(int code,int result,Intent intent){super.onActivityResult(code,result,intent);if(code==PICK_FILE&&fileCallback!=null){fileCallback.onReceiveValue(result==RESULT_OK&&intent!=null&&intent.getData()!=null?new Uri[]{intent.getData()}:null);fileCallback=null;}if(code==SAVE_FILE){if(result==RESULT_OK&&intent!=null&&intent.getData()!=null&&pendingDocument!=null)try(OutputStream stream=getContentResolver().openOutputStream(intent.getData())){if(stream!=null)stream.write(pendingDocument);}catch(Exception e){Toast.makeText(this,"تعذر كتابة الملف",Toast.LENGTH_LONG).show();}pendingDocument=null;}}
    @Override public void onBackPressed(){if(web!=null&&web.canGoBack())web.goBack();else new AlertDialog.Builder(this).setTitle("إغلاق وصلة؟").setMessage("تأكد من مزامنة تأكيدات الركوب قبل الخروج.").setPositiveButton("إغلاق",(d,w)->finish()).setNegativeButton("متابعة",null).show();}
    @Override protected void onDestroy(){if(fileCallback!=null)fileCallback.onReceiveValue(null);if(web!=null){web.removeJavascriptInterface("WaslaAndroid");web.destroy();}super.onDestroy();}
}
