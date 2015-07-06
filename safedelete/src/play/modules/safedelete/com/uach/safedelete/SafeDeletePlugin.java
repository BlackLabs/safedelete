package com.uach.safedelete;

import play.PlayPlugin;
import play.classloading.ApplicationClasses.ApplicationClass;

/**
 * Clase para realizar el enhance sobre las clases que utilicen éste módulo.
 * 
 * @author Edgar Hiram Hernández Reyes
 */
public class SafeDeletePlugin extends PlayPlugin {

    @Override
    public void enhance(ApplicationClass applicationClass) throws Exception {
        new SafeDeleteEnhancer().enhanceThisClass(applicationClass);
    }
}
