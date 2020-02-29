package com.github.monkeywie.proxyee;

import java.util.Vector;

public class Task4Search implements Runnable{

    @Override
    public void run(){
        //System.out.println(Thread.currentThread().getName());
        Vector<String> ret = HttpProxyServerApp.queue4Search.poll();
        if(ret == null||ret.size() == 0){
            return ;
        }

        for(int i = 0; i < ret.size(); i++){
            System.out.println(ret.get(i));
        }
    }
}
