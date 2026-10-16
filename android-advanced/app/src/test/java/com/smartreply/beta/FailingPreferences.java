package com.smartreply.beta;

import android.content.SharedPreferences;
import java.lang.reflect.Proxy;

/** Simulates a disk commit failure without changing the underlying durable store. */
final class FailingPreferences {
    static SharedPreferences wrap(SharedPreferences target) {
        return (SharedPreferences)Proxy.newProxyInstance(SharedPreferences.class.getClassLoader(),
            new Class<?>[]{SharedPreferences.class},(proxy,method,args)->{
                if(method.getName().equals("edit")){
                    SharedPreferences.Editor editor=target.edit();
                    return Proxy.newProxyInstance(SharedPreferences.Editor.class.getClassLoader(),
                        new Class<?>[]{SharedPreferences.Editor.class},(ep,em,ea)->{
                            if(em.getName().equals("commit"))return false;
                            if(em.getName().equals("apply"))return null;
                            Object value=em.invoke(editor,ea);
                            return value instanceof SharedPreferences.Editor ? ep : value;
                        });
                }
                return method.invoke(target,args);
            });
    }
}
