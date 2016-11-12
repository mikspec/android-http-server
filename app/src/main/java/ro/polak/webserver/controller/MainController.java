/**************************************************
 * Android Web Server
 * Based on JavaLittleWebServer (2008)
 * <p/>
 * Copyright (c) Piotr Polak 2008-2016
 **************************************************/

package ro.polak.webserver.controller;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import ro.polak.webserver.MimeTypeMapping;
import ro.polak.webserver.ServerConfig;
import ro.polak.webserver.WebServer;
import ro.polak.webserver.gui.ServerGui;
import ro.polak.webserver.impl.ServerConfigImpl;
import ro.polak.webserver.resource.provider.AssetResourceProvider;
import ro.polak.webserver.resource.provider.FileResourceProvider;
import ro.polak.webserver.resource.provider.ResourceProvider;
import ro.polak.webserver.resource.provider.ServletResourceProvider;
import ro.polak.webserver.servlet.ServletContextWrapper;
import ro.polak.webserver.session.storage.FileSessionStorage;

/**
 * The main controller of the server, can only be initialized as a singleton
 *
 * @author Piotr Polak piotr [at] polak [dot] ro
 * @since 201012
 */
public class MainController implements Controller {

    private static final Logger LOGGER = Logger.getLogger(MainController.class.getName());

    private WebServer webServer;
    private ServerGui gui;
    private Object context;
    private static MainController instance;

    /**
     * Making the controller constructor private for singleton
     */
    private MainController() {
        Thread.currentThread().setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                final String originalClass = ex.getStackTrace()[0].getClassName();
                Logger.getLogger(originalClass).log(Level.SEVERE, "Exception", ex);
            }
        });
    }

    /**
     * Singleton method
     *
     * @return
     */
    public static MainController getInstance() {
        if (MainController.instance == null) {
            MainController.instance = new MainController();
        }

        return MainController.instance;
    }

    /**
     * Sets server GUI
     *
     * @param gui
     */
    public void setGui(final ServerGui gui) {
        this.gui = gui;
    }

    @Override
    public void start() {
        gui.initialize(this);
        try {
            String baseConfigPath;
            if (getAndroidContext() != null) {
                baseConfigPath = Environment.getExternalStorageDirectory() + "/httpd/";
            } else {
                baseConfigPath = "./app/src/main/assets/conf/";
            }

            ServerConfig serverConfig;
            serverConfig = getServerConfig(baseConfigPath);

            webServer = new WebServer(new ServerSocket(), serverConfig);
            if (webServer.startServer()) {
                gui.start();
            }
        } catch (IOException e) {
        }
    }

    @Override
    public void stop() {
        if (webServer != null) {
            webServer.stopServer();
            webServer = null;
        }
        gui.stop();
    }

    @Override
    public WebServer getWebServer() {
        return webServer;
    }

    @Override
    public Object getAndroidContext() {
        return context;
    }

    @Override
    public void setAndroidContext(Object context) {
        this.context = context;
    }

    private ServerConfig getServerConfig(String baseConfigPath) {
        ServerConfigImpl serverConfig;
        try {
            serverConfig = ServerConfigImpl.createFromPath(baseConfigPath, System.getProperty("java.io.tmpdir"));
        } catch (IOException e) {
            LOGGER.warning("Unable to read server config. Using the default configuration.");
            serverConfig = new ServerConfigImpl();
        }

        serverConfig.setResourceProviders(selectActiveResourceProviders(serverConfig));
        return serverConfig;
    }

    /**
     * For performance reasons ServletResourceProvider is the last resource provider.
     *
     * @param serverConfig
     * @return
     */
    private ResourceProvider[] selectActiveResourceProviders(ServerConfig serverConfig) {
        List<ResourceProvider> resourceProviders = new ArrayList<>();

        resourceProviders.add(getFileResourceProvider(serverConfig));
        resourceProviders.add(getAssetsResourceProvider(serverConfig.getMimeTypeMapping()));
        resourceProviders.add(getServletResourceProvider(serverConfig));
        return resourceProviders.toArray(new ResourceProvider[resourceProviders.size()]);
    }

    private FileResourceProvider getFileResourceProvider(ServerConfig serverConfig) {
        return new FileResourceProvider(serverConfig.getMimeTypeMapping(),
                serverConfig.getDocumentRootPath());
    }

    private ServletResourceProvider getServletResourceProvider(ServerConfig serverConfig) {
        return new ServletResourceProvider(
                new ServletContextWrapper(serverConfig,
                        new FileSessionStorage(serverConfig.getTempPath())),
                serverConfig.getServletMappedExtension());
    }

    private ResourceProvider getAssetsResourceProvider(MimeTypeMapping mimeTypeMapping) {
        String assetBasePath = "public";
        if (getAndroidContext() != null) {
            AssetManager assetManager = ((Context) getAndroidContext()).getResources().getAssets();
            return new AssetResourceProvider(assetManager, assetBasePath);
        } else {
            return new FileResourceProvider(mimeTypeMapping, "./app/src/main/assets/" + assetBasePath);
        }
    }
}
