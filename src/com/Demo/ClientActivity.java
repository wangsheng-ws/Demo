package com.Demo;

import sec.vpn.ICertService_DeviceSuitInfo;
import sec.vpn.ICertService_TfInfo;
import sec.vpn.ISecVpnService;

import com.cr.communication.IDaemonService;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ScrollView;
import android.widget.TextView;


/*
 * ClientActivity演示了调用VPN客户端和终端通讯组件接口，接收广播消息等操作
 * 
* 接口调用步骤：
 *  1. 将IDaemonService.aidl放到src/com/cr/communication目录下,ISecVpnService.aidl 放到src/sec/vpn目录下
 *  2. 在Activity中创建IDaemonService,ISecVpnService对象
 *  2. 创建ServiceConnection链接点并初始化IDaemonService,ISecVpnService对象
* 3.onCreate中先调用startService，再调用bindService
* 4.onDestroy中调用unbindService
* 
*  接收广播消息（可选）：
*  1. 在Activity中创建广播接收器
*  2. 在onCreate中注册广播过滤器
*  3. 在onDestory中注销广播接收器
* 注：广播接收器中不可进行大量耗时的操作，一般将此类操作放入线程中运行
* 
* 接口定义：
* 见 IDaemonService.aidl 和 ISecVpnService.aidl
 */

public class ClientActivity extends Activity implements OnClickListener{
	// SSL广播消息数据
	public static final String ACTION_INTENT_DATA="data";
	// SSL广播消息：启动服务中
	public static final String ACTION_INTENT_STARTSERVER_INPROC="koal.ssl.broadcast.startserver.inproc";
	// SSL广播消息：启动服务成功
	public static final String ACTION_INTENT_STARTSERVER_SUCCESS="koal.ssl.broadcast.startserver.success";
	// SSL广播消息：启动服务失败
	public static final String ACTION_INTENT_STARTSERVER_FAILURE="koal.ssl.broadcast.startserver.failure";
	// SSL广播消息：下载策略成功
	public static final String ACTION_INTENT_DOWNLOADCFG_SUCCESS="koal.ssl.broadcast.downloadcfg.success";
	// SSL广播消息：停止服务成功
	public static final String ACTION_INTENT_STOPSERVER_SUCCESS="koal.ssl.broadcast.stopserver.success";
	// SSL广播消息：检测到新版本，可升级
	public static final String ACTION_INTENT_UPGRADE="koal.ssl.broadcast.upgrade";
	// SSL广播消息：网络（wifi/apn）已链接 
	public static final String ACTION_INTENT_NETWORK_CONNECTED="koal.ssl.broadcast.network.connected";
	// SSL广播消息：网络（wifi/apn）已断开
	public static final String ACTION_INTENT_NETWORK_DISCONNECTED="koal.ssl.broadcast.network.disconnected";
	// SSL广播消息：隧道已建立
	public static final String ACTION_INTENT_TUNNEL_CONNECTED = "koal.ssl.broadcast.tunnel.connected";
	// SSL广播消息：隧道已断开
	public static final String ACTION_INTENT_TUNNEL_FAILURE = "koal.ssl.broadcast.tunnel.failure";
	// SSL广播消息：隧道已断开（认证错误）
	public static final String ACTION_INTENT_TUNNEL_FAILURE_AUTH = "koal.ssl.broadcast.tunnel.failure.auth";
	// 终端通讯组件广播消息：运营商网络可达
	public static final String ACTION_INTENT_CHECKCARRIERS_SUCCESS = "com.cr.communication.broadcast.checkcarriers.success";
	// 终端通讯组件广播消息：运营商网络不可达
	public static final String ACTION_INTENT_CHECKCARRIERS_FAILURE = "com.cr.communication.broadcast.checkcarriers.failure";
	// 安全VPN服务名称
	private static String SEC_VPN_SERVICE_ACTION_NAME = "sec.vpn.service";
	// 终端通讯组件服务名称
	public static final String DAEMON_SERVICE_ACTION_NAME = "com.cr.communication.DaemonService";
	
	private static final String MSG_KEY="data";
	private static final int MSG_SHOWLOG = 1;
	private static final int MSG_UPGRADE = 2;
	

	private TextView txtLog = null;
	private ScrollView sclView = null;
	
	private ServiceMon srvMonitor = null;
	private IDaemonService daemonService = null;
	private ISecVpnService secVpnService = null;
	
	private ServiceConnection daemonServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service)
		{
			// TODO Auto-generated method stub
			daemonService = IDaemonService.Stub.asInterface(service);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub
			daemonService = null;
		}
	};
	
	private ServiceConnection secVpnServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service)
		{
			// TODO Auto-generated method stub
			secVpnService = ISecVpnService.Stub.asInterface(service);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub
			secVpnService = null;
		}
	};
	
	
	
	private Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			String data = msg.getData().getString(MSG_KEY);;
			
			switch(msg.what) {
				case MSG_SHOWLOG:			// 向Log控件输出数据
					AppendLog(data);
					break;
				default:
					break;
			}
		};
	};
	
	
	private void handleMessage(Handler h, int msgID, String data)
	{
		Message msg = new Message();
		msg.what = msgID;
		Bundle bundle = new Bundle();
		bundle.putString(MSG_KEY, data);
		msg.setData(bundle);
		handler.sendMessage(msg);
	}
	
	
	
	/* SSL广播接收器
	 *  为防止接收器的阻塞，最好将耗时的操作放入handle中完成
	 */
	private class ServiceMon extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String data = intent.getStringExtra(ACTION_INTENT_DATA);
			if (intent.getAction().equals(ACTION_INTENT_STARTSERVER_INPROC)) {
				handleMessage(handler, MSG_SHOWLOG, data);
			} 
			else if (intent.getAction().equals(ACTION_INTENT_STARTSERVER_SUCCESS)) {
				handleMessage(handler, MSG_SHOWLOG, "启动服务成功！");
			}
			else if (intent.getAction().equals(ACTION_INTENT_STARTSERVER_FAILURE)) {
				handleMessage(handler, MSG_SHOWLOG, "启动服务失败！");
			}
			else if (intent.getAction().equals(ACTION_INTENT_DOWNLOADCFG_SUCCESS)) {
				handleMessage(handler, MSG_SHOWLOG, "下载策略成功！");
			}
			else if (intent.getAction().equals(ACTION_INTENT_STOPSERVER_SUCCESS)) {
				handleMessage(handler, MSG_SHOWLOG, "停止服务成功！");
			}
			else if (intent.getAction().equals(ACTION_INTENT_UPGRADE)) {
				handleMessage(handler, MSG_UPGRADE, data);
			}
			else if (intent.getAction().equals(ACTION_INTENT_NETWORK_CONNECTED)) {
				handleMessage(handler, MSG_SHOWLOG, "网络已链接");
			}
			else if (intent.getAction().equals(ACTION_INTENT_NETWORK_DISCONNECTED)) {
				handleMessage(handler, MSG_SHOWLOG, "网络已断开");
			}
			else if (intent.getAction().equals(ACTION_INTENT_TUNNEL_CONNECTED)) {
				handleMessage(handler, MSG_SHOWLOG, "隧道已建立" );
			}
			else if (intent.getAction().equals(ACTION_INTENT_TUNNEL_FAILURE)||
					intent.getAction().equals(ACTION_INTENT_TUNNEL_FAILURE_AUTH) ) {
				handleMessage(handler, MSG_SHOWLOG, "隧道已断开" );
			}
			else if (intent.getAction().equals(ACTION_INTENT_CHECKCARRIERS_SUCCESS)) {
				handleMessage(handler, MSG_SHOWLOG, "运营商网络可达" );
			}
			else if (intent.getAction().equals(ACTION_INTENT_CHECKCARRIERS_FAILURE)) {
				handleMessage(handler, MSG_SHOWLOG, "运营商网络不可达" );
			}
			
		}
	}
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		findViewById(R.id.btnStart).setOnClickListener(this);
		findViewById(R.id.btnStop).setOnClickListener(this);
		findViewById(R.id.btnState).setOnClickListener(this);
		findViewById(R.id.btnCheckLinkState).setOnClickListener(this);
		findViewById(R.id.btnQuit).setOnClickListener(this);
		findViewById(R.id.btnGetTfInfo).setOnClickListener(this);
		findViewById(R.id.btnGetDevSuitInfo).setOnClickListener(this);
		
		txtLog = (TextView)findViewById(R.id.txtLog);
		sclView = (ScrollView)findViewById(R.id.sclView);
		
		
		// 广播接收器，用来监听SSL服务发出的广播
		srvMonitor = new ServiceMon();
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_INTENT_STARTSERVER_INPROC);
		filter.addAction(ACTION_INTENT_STARTSERVER_SUCCESS);
		filter.addAction(ACTION_INTENT_STARTSERVER_FAILURE);
		filter.addAction(ACTION_INTENT_DOWNLOADCFG_SUCCESS);
		filter.addAction(ACTION_INTENT_STOPSERVER_SUCCESS);
		filter.addAction(ACTION_INTENT_NETWORK_CONNECTED);
		filter.addAction(ACTION_INTENT_NETWORK_DISCONNECTED);
		filter.addAction(ACTION_INTENT_TUNNEL_CONNECTED);
		filter.addAction(ACTION_INTENT_TUNNEL_FAILURE);
		filter.addAction(ACTION_INTENT_TUNNEL_FAILURE_AUTH);
		filter.addAction(ACTION_INTENT_CHECKCARRIERS_SUCCESS);
		filter.addAction(ACTION_INTENT_CHECKCARRIERS_FAILURE);
		registerReceiver(srvMonitor, filter);
		
		// 绑定安全VPN服务
		Intent intent = new Intent(SEC_VPN_SERVICE_ACTION_NAME);
//如安卓6.0及以上需要加上：intent.setPackage("km.ssl");		
		startService(intent);
		bindService(intent, secVpnServiceConnection, BIND_AUTO_CREATE);
		
		// 绑定终端通讯组件服务
		intent = new Intent(DAEMON_SERVICE_ACTION_NAME);		
		startService(intent);
		bindService(intent, daemonServiceConnection, BIND_AUTO_CREATE);
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		
		unregisterReceiver(srvMonitor);
		// 取消绑定安全VPN服务
		unbindService(secVpnServiceConnection);
		// 取消绑定终端通讯组件服务
		unbindService(daemonServiceConnection);
	}
	
	@Override
	public void onClick(View v)
	{
		// TODO Auto-generated method stub
		try
		{
			switch (v.getId())
			{
			case R.id.btnStart:
				if (!secVpnService.sv_isStarted())
				{
					secVpnService.sv_setServerAddr("20.50.0.11", "10009");
					secVpnService.sv_start();
				}
				break;

			case R.id.btnStop:
				secVpnService.sv_stop();
				break;

			case R.id.btnState:
				AppendLog(secVpnService.sv_serviceState());
				break;
			case R.id.btnGetTfInfo:
				ICertService_TfInfo mICertService_TfInfo = secVpnService.getTfInfo();
				String result = mICertService_TfInfo.getResult();
				if ("0".equals(result))
				{
					AppendLog(mICertService_TfInfo.getCertSn());
					AppendLog(mICertService_TfInfo.getCertOu());
					AppendLog(mICertService_TfInfo.getMedium());
					AppendLog(mICertService_TfInfo.getNotAfter());
					AppendLog(mICertService_TfInfo.getNotBefore());
					AppendLog(mICertService_TfInfo.getTfSn());
					AppendLog(mICertService_TfInfo.getUserID());
					AppendLog(mICertService_TfInfo.getUserName());
				}
				else
				{
					AppendLog("getTfInfo 失败:"+result);
				}
				break;
				
			case R.id.btnGetDevSuitInfo:
				ICertService_DeviceSuitInfo mICertService_DeviceSuitInfo = secVpnService.getDeviceSuitInfo();
				String result2 = mICertService_DeviceSuitInfo.getResult();
				if ("0".equals(result2))
				{
					AppendLog(mICertService_DeviceSuitInfo.getCertSn());
					AppendLog(mICertService_DeviceSuitInfo.getTfSn());
					AppendLog(mICertService_DeviceSuitInfo.getIccid());
					AppendLog(mICertService_DeviceSuitInfo.getImei());
				}
				else
				{
					AppendLog("getDeviceSuitInfo 失败:"+result2);
				}
				break;
				

			case R.id.btnCheckLinkState:
				AppendLog("B64:");
				AppendLog(secVpnService.sv_getCertBase64(true));
				AppendLog("No-B64:");
				AppendLog(secVpnService.sv_getCertBase64(false));
//				daemonService.CheckCarriers();
				break;

			case R.id.btnQuit:
				secVpnService.sv_quit();
				finish();
				break;
			default:
				break;
			}
		}
		catch (RemoteException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	void AppendLog(String strLog)
	{
		txtLog.append(strLog + "\n");
		// 自动滚屏
		sclView.post(new Runnable()	{
			@Override
			public void run()	{
				sclView.fullScroll(ScrollView.FOCUS_DOWN);
			}
		});	
	}
}

