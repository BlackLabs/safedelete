package com.uach.safedelete;

import play.PlayPlugin;
import play.classloading.ApplicationClasses.ApplicationClass;

/**
 * Play! enhancer for SafeDelete.
 * 
 * @author Edgar Hiram Hernández Reyes
 * @version 0.1
 */
public class SafeDeletePlugin extends PlayPlugin {

    @Override
    public void enhance(ApplicationClass applicationClass) throws Exception {
        new SafeDeleteEnhancer().enhanceThisClass(applicationClass);
    }
}
