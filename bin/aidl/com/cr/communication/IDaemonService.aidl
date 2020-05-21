package com.cr.communication;

interface IDaemonService {

    /*
           运营商网络连通性检测：
           默认检测IP列表：
		 172.18.2.100
		 172.18.6.1
		 172.18.100.1
		 172.18.5.1
           方式：同时检测四个IP地址，每个IP地址检测一次，超时2秒
           返回值：以广播形式返回
     	成功：（有一个ip地址可达，就表示成功）
    		"com.cr.communication.broadcast.checkcarriers.success"
    	失败： （四个ip地址均不可达，就表示失败）
    		"com.cr.communication.broadcast.checkcarriers.failure"
    */

	void CheckCarriers();

}