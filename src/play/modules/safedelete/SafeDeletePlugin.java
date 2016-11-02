package play.modules.safedelete;

import play.PlayPlugin;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.Logger;

/**
 * Play! enhancer for SafeDelete.
 *
 * @author Edgar Hiram Hernández Reyes
 * @version 0.1
 */
public class SafeDeletePlugin extends PlayPlugin {

    private SafeDeleteEnhancer safeDeleteEnhancer = new SafeDeleteEnhancer();

    @Override
    public void enhance(ApplicationClass applicationClass) throws Exception {
        safeDeleteEnhancer.enhanceThisClass(applicationClass);
    }
}
