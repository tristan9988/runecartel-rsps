package com.runecartel.launcher;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.zip.*;

/**
 * RuneCartel's HQ Game Launcher
 * Self-contained launcher that embeds client and cache.
 * Checks GitHub for updates, downloads if newer, falls back to embedded resources.
 */
public class Launcher extends JFrame {

    private static final EmbeddedVersions EMBEDDED_VERSIONS = loadEmbeddedVersions();

    // Embedded versions are sourced from /embedded/version.properties so the build/publish
    // pipeline can keep launcher fallbacks in sync automatically.
    private static final int CACHE_VERSION = EMBEDDED_VERSIONS.cacheVersion;
    private static final int CLIENT_VERSION = EMBEDDED_VERSIONS.clientVersion;
    private static final int LAUNCHER_VERSION = EMBEDDED_VERSIONS.launcherVersion;

    // ========== CHANGE THIS TO YOUR GITHUB REPO ==========
    // Format: https://github.com/YOUR_USERNAME/YOUR_REPO/releases/download/latest/
    private static final String REMOTE_BASE_URL = "https://github.com/tristan9988/runecartel-updates/releases/download/latest/";
    // =====================================================
    private static final boolean USE_REMOTE_UPDATES = true;

    private static final String CLIENT_JAR = "RuneCartel.jar";
    private static final String GAME_DIRECTORY = System.getProperty("user.home") + File.separator + ".runecartel";
    private static final String CACHE_DIRECTORY = GAME_DIRECTORY + File.separator + "cache";
    private static final String VERSION_FILE = "version.properties";
    private static final String PREFERRED_JAVA_PATH = "C:\\Program Files\\Eclipse Adoptium\\jdk-11.0.22.7-hotspot\\bin\\java.exe";

    // Embedded resource paths (inside the JAR)
    private static final String EMBEDDED_CLIENT_ZIP = "/embedded/client.zip";
    private static final String EMBEDDED_CACHE_ZIP = "/embedded/cache.zip";
    private static final String EMBEDDED_VERSION_FILE = "/embedded/version.properties";
    
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private BufferedImage logoImage;

    public Launcher() {
        loadLogo();
        initUI();
        // Start installation/update process
        new Thread(this::installAndLaunch).start();
    }
    
    private void loadLogo() {
        try {
            InputStream is = getClass().getResourceAsStream("/logo.png");
            if (is != null) {
                logoImage = ImageIO.read(is);
                is.close();
            }
        } catch (Exception e) {
            System.out.println("Could not load logo: " + e.getMessage());
        }
    }

    private void initUI() {
        setTitle("RuneCartel's HQ");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(450, 300);
        setLocationRelativeTo(null);
        setResizable(false);
        
        if (logoImage != null) {
            setIconImage(logoImage);
        }
        
        // Main panel with gradient background
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gradient = new GradientPaint(0, 0, new Color(40, 15, 25), 
                                                            0, getHeight(), new Color(15, 5, 10));
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        
        // Top panel with logo and title
        JPanel topPanel = new JPanel();
        topPanel.setOpaque(false);
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        
        if (logoImage != null) {
            int logoSize = 100;
            Image scaledLogo = logoImage.getScaledInstance(logoSize, logoSize, Image.SCALE_SMOOTH);
            JLabel logoLabel = new JLabel(new ImageIcon(scaledLogo));
            logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            topPanel.add(logoLabel);
            topPanel.add(Box.createVerticalStrut(10));
        }
        
        JLabel titleLabel = new JLabel("RuneCartel's HQ", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(new Color(255, 215, 0));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.add(titleLabel);
        
        mainPanel.add(topPanel, BorderLayout.NORTH);
        
        // Center panel with status and progress
        JPanel centerPanel = new JPanel();
        centerPanel.setOpaque(false);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        
        centerPanel.add(Box.createVerticalStrut(20));
        
        statusLabel = new JLabel("Initializing...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(statusLabel);
        
        centerPanel.add(Box.createVerticalStrut(15));
        
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(350, 25));
        progressBar.setMaximumSize(new Dimension(350, 25));
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        progressBar.setForeground(new Color(0, 150, 0));
        centerPanel.add(progressBar);
        
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        
        // Version label
        JLabel versionLabel = new JLabel("v" + CLIENT_VERSION + "." + CACHE_VERSION, SwingConstants.CENTER);
        versionLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        versionLabel.setForeground(new Color(100, 100, 100));
        mainPanel.add(versionLabel, BorderLayout.SOUTH);
        
        add(mainPanel);
    }
    
    private void installAndLaunch() {
        try {
            // Create directories
            Files.createDirectories(Paths.get(GAME_DIRECTORY));
            Files.createDirectories(Paths.get(CACHE_DIRECTORY));
            
            // Load local version info
            int localClientVersion = 0;
            int localCacheVersion = 0;
            File versionFile = new File(GAME_DIRECTORY + File.separator + VERSION_FILE);
            
            if (versionFile.exists()) {
                try (FileInputStream fis = new FileInputStream(versionFile)) {
                    Properties props = new Properties();
                    props.load(fis);
                    localClientVersion = Integer.parseInt(props.getProperty("client.version", "0"));
                    localCacheVersion = Integer.parseInt(props.getProperty("cache.version", "0"));
                } catch (Exception e) {
                    // Reset versions if file is corrupted
                }
            }
            
            // Check for remote updates first
            int remoteClientVersion = -1;
            int remoteCacheVersion = -1;
            int remoteLauncherVersion = -1;

            updateStatus("Checking for updates...");
            setProgress(5);
            
            // Check for launcher self-update FIRST
            if (USE_REMOTE_UPDATES) {
                Properties remoteProps = fetchRemoteVersions();
                if (remoteProps != null) {
                    remoteLauncherVersion = Integer.parseInt(remoteProps.getProperty("launcher.version", "-1"));
                    if (remoteLauncherVersion > LAUNCHER_VERSION) {
                        updateStatus("Updating launcher to v" + remoteLauncherVersion + "...");
                        setProgress(10);
                        if (downloadAndReplaceLauncher()) {
                            updateStatus("Launcher updated! Restarting...");
                            Thread.sleep(1000);
                            restartLauncher();
                            return;
                        }
                    }
                }
            }

            Properties remoteProps = USE_REMOTE_UPDATES ? fetchRemoteVersions() : null;
            if (remoteProps != null) {
                remoteClientVersion = Integer.parseInt(remoteProps.getProperty("client.version", "-1"));
                remoteCacheVersion = Integer.parseInt(remoteProps.getProperty("cache.version", "-1"));
                System.out.println("Remote versions - client: " + remoteClientVersion + ", cache: " + remoteCacheVersion);
                System.out.println("Local versions  - client: " + localClientVersion + ", cache: " + localCacheVersion);
            } else {
                System.out.println("Could not fetch remote versions, using embedded resources as fallback.");
            }
            
            boolean needsClientUpdate = false;
            boolean needsCacheUpdate = false;
            boolean useRemote = false;
            
            File clientFile = new File(GAME_DIRECTORY + File.separator + CLIENT_JAR);
            File cacheDir = new File(CACHE_DIRECTORY);
            File[] cacheFiles = cacheDir.listFiles();
            boolean cacheEmpty = (cacheFiles == null || cacheFiles.length < 5);
            
            // Determine update strategy
            if (remoteClientVersion > 0 && remoteClientVersion > localClientVersion) {
                needsClientUpdate = true;
                useRemote = true;
            } else if (!clientFile.exists()) {
                needsClientUpdate = true;
                // Use remote if available and newer than embedded, else embedded
                useRemote = (remoteClientVersion > CLIENT_VERSION);
            } else if (CLIENT_VERSION > localClientVersion) {
                needsClientUpdate = true;
                useRemote = false;
            }
            
            if (remoteCacheVersion > 0 && remoteCacheVersion > localCacheVersion) {
                needsCacheUpdate = true;
                useRemote = true;
            } else if (cacheEmpty) {
                needsCacheUpdate = true;
                // Use remote if available and newer than embedded, else embedded
                if (remoteCacheVersion <= CACHE_VERSION) useRemote = false;
            } else if (CACHE_VERSION > localCacheVersion) {
                needsCacheUpdate = true;
                useRemote = false;
            }
            
            int effectiveClientVersion = CLIENT_VERSION;
            int effectiveCacheVersion = CACHE_VERSION;
            
            // Update client if needed
            if (needsClientUpdate) {
                boolean downloaded = false;
                
                if (useRemote && remoteClientVersion > 0) {
                    updateStatus("Downloading client update v" + remoteClientVersion + "...");
                    setProgress(10);
                    downloaded = downloadFile(REMOTE_BASE_URL + "client.zip", 
                                             Paths.get(GAME_DIRECTORY, "client_update.zip"));
                    
                    if (downloaded) {
                        updateStatus("Installing client update...");
                        setProgress(30);
                        extracted_client_from_local_zip(new File(GAME_DIRECTORY + File.separator + "client_update.zip"));
                        new File(GAME_DIRECTORY + File.separator + "client_update.zip").delete();
                        effectiveClientVersion = remoteClientVersion;
                    }
                }
                
                if (!downloaded) {
                    // Fallback to embedded
                    updateStatus("Installing client...");
                    setProgress(10);
                    if (extractClientFromZip()) {
                        effectiveClientVersion = CLIENT_VERSION;
                    } else {
                        updateStatus("Error: Could not extract client!");
                        return;
                    }
                }
                setProgress(40);
            }
            
            // Update cache if needed
            if (needsCacheUpdate) {
                boolean downloaded = false;
                
                // Delete old cache
                updateStatus("Removing old cache...");
                deleteDirectory(new File(CACHE_DIRECTORY));
                Files.createDirectories(Paths.get(CACHE_DIRECTORY));
                
                if (useRemote && remoteCacheVersion > 0) {
                    updateStatus("Downloading cache update v" + remoteCacheVersion + "...");
                    setProgress(50);
                    downloaded = downloadFile(REMOTE_BASE_URL + "cache.zip", 
                                             Paths.get(GAME_DIRECTORY, "cache_update.zip"));
                    
                    if (downloaded) {
                        updateStatus("Installing cache update...");
                        setProgress(70);
                        extractLocalZipToCache(new File(GAME_DIRECTORY + File.separator + "cache_update.zip"));
                        new File(GAME_DIRECTORY + File.separator + "cache_update.zip").delete();
                        effectiveCacheVersion = remoteCacheVersion;
                    }
                }
                
                if (!downloaded) {
                    // Fallback to embedded
                    updateStatus("Installing cache files...");
                    setProgress(50);
                    if (extractEmbeddedCache()) {
                        effectiveCacheVersion = CACHE_VERSION;
                    } else {
                        updateStatus("Error: Could not extract cache!");
                        return;
                    }
                }
                setProgress(85);
            }
            
            if (!needsClientUpdate && !needsCacheUpdate) {
                updateStatus("Game is up to date!");
                setProgress(90);
            }
            
            // Save version info (use the highest version we got)
            int finalClientVer = Math.max(effectiveClientVersion, localClientVersion);
            int finalCacheVer = Math.max(effectiveCacheVersion, localCacheVersion);
            saveVersionInfo(finalClientVer, finalCacheVer);
            
            setProgress(100);
            updateStatus("Launching game...");
            Thread.sleep(500);
            
            // Launch the game
            launchGame();
            
        } catch (Exception e) {
            e.printStackTrace();
            updateStatus("Error: " + e.getMessage());
        }
    }
    
    /**
     * Fetches remote version.properties from GitHub.
     * Returns null if unavailable (no internet, repo not set up, etc.)
     */
    private Properties fetchRemoteVersions() {
        try {
            String url = REMOTE_BASE_URL + "version.properties";
            HttpURLConnection conn = openConnection(url);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            if (conn.getResponseCode() == 200) {
                Properties props = new Properties();
                try (InputStream is = conn.getInputStream()) {
                    props.load(is);
                }
                conn.disconnect();
                return props;
            }
            conn.disconnect();
        } catch (Exception e) {
            System.out.println("Could not check for updates: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Downloads a file from a URL, following redirects (GitHub uses 302s).
     * Returns true if successful.
     */
    private boolean downloadFile(String urlStr, Path destPath) {
        try {
            HttpURLConnection conn = openConnection(urlStr);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                System.out.println("Download failed, HTTP " + responseCode + " for " + urlStr);
                conn.disconnect();
                return false;
            }
            
            long totalSize = conn.getContentLengthLong();
            
            try (InputStream is = conn.getInputStream();
                 FileOutputStream fos = new FileOutputStream(destPath.toFile())) {
                byte[] buffer = new byte[8192];
                long downloaded = 0;
                int len;
                while ((len = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                    downloaded += len;
                    if (totalSize > 0) {
                        int percent = (int) ((downloaded * 100) / totalSize);
                        // Update sub-progress within the current phase
                        final int p = percent;
                        SwingUtilities.invokeLater(() -> progressBar.setString(p + "%"));
                    }
                }
            }
            conn.disconnect();
            System.out.println("Downloaded: " + destPath.getFileName());
            return true;
        } catch (Exception e) {
            System.out.println("Download error: " + e.getMessage());
            // Clean up partial download
            try { Files.deleteIfExists(destPath); } catch (Exception ignored) {}
            return false;
        }
    }
    
    /**
     * Opens an HTTP connection, manually following up to 5 redirects.
     * GitHub Releases redirect to CDN, and Java doesn't follow cross-protocol (http→https) redirects.
     */
    private HttpURLConnection openConnection(String urlStr) throws IOException {
        int redirects = 0;
        while (redirects < 5) {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "RuneCartel-Launcher");
            conn.setInstanceFollowRedirects(false);
            
            int code = conn.getResponseCode();
            if (code == HttpURLConnection.HTTP_MOVED_PERM || 
                code == HttpURLConnection.HTTP_MOVED_TEMP ||
                code == 307 || code == 308) {
                String newUrl = conn.getHeaderField("Location");
                conn.disconnect();
                if (newUrl == null) throw new IOException("Redirect with no Location header");
                urlStr = newUrl;
                redirects++;
            } else {
                return conn;
            }
        }
        throw new IOException("Too many redirects");
    }
    
    /**
     * Extracts a client JAR from a locally downloaded zip file.
     */
    private boolean extracted_client_from_local_zip(File zipFile) {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().endsWith(".jar")) {
                    File destFile = new File(GAME_DIRECTORY + File.separator + CLIENT_JAR);
                    try (FileOutputStream fos = new FileOutputStream(destFile)) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                    zis.closeEntry();
                    return true;
                }
                zis.closeEntry();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Extracts a locally downloaded cache zip to the cache directory.
     */
    private boolean extractLocalZipToCache(File zipFile) {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File newFile = new File(CACHE_DIRECTORY + File.separator + entry.getName());
                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    new File(newFile.getParent()).mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    
    private boolean extractEmbeddedFile(String resourcePath, String destPath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                System.out.println("Resource not found: " + resourcePath);
                return false;
            }
            
            Files.copy(is, Paths.get(destPath), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private boolean extractClientFromZip() {
        try (InputStream is = getClass().getResourceAsStream(EMBEDDED_CLIENT_ZIP)) {
            if (is == null) {
                System.out.println("Client zip not found in embedded resources");
                return false;
            }
            
            try (ZipInputStream zis = new ZipInputStream(is)) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.getName().endsWith(".jar")) {
                        // Extract the JAR file
                        File destFile = new File(GAME_DIRECTORY + File.separator + CLIENT_JAR);
                        try (FileOutputStream fos = new FileOutputStream(destFile)) {
                            byte[] buffer = new byte[8192];
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                        }
                        zis.closeEntry();
                        return true;
                    }
                    zis.closeEntry();
                }
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private boolean extractEmbeddedCache() {
        try (InputStream is = getClass().getResourceAsStream(EMBEDDED_CACHE_ZIP)) {
            if (is == null) {
                System.out.println("Embedded cache.zip not found, trying individual files...");
                return extractIndividualCacheFiles();
            }
            
            // Extract zip to cache directory
            try (ZipInputStream zis = new ZipInputStream(is)) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    File newFile = new File(CACHE_DIRECTORY + File.separator + entry.getName());
                    
                    if (entry.isDirectory()) {
                        newFile.mkdirs();
                    } else {
                        new File(newFile.getParent()).mkdirs();
                        try (FileOutputStream fos = new FileOutputStream(newFile)) {
                            byte[] buffer = new byte[8192];
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                        }
                    }
                    zis.closeEntry();
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return extractIndividualCacheFiles();
        }
    }
    
    private boolean extractIndividualCacheFiles() {
        String[] cacheFileNames = {
            "main_file_cache.dat",
            "main_file_cache.idx0",
            "main_file_cache.idx1",
            "main_file_cache.idx2",
            "main_file_cache.idx3",
            "main_file_cache.idx4",
            "main_file_cache.idx5",
            "main_file_sprites.dat",
            "main_file_sprites.idx"
        };
        
        boolean anyExtracted = false;
        for (String fileName : cacheFileNames) {
            String resourcePath = "/embedded/cache/" + fileName;
            try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
                if (is != null) {
                    Files.copy(is, Paths.get(CACHE_DIRECTORY + File.separator + fileName), 
                              StandardCopyOption.REPLACE_EXISTING);
                    anyExtracted = true;
                }
            } catch (Exception e) {
                // Continue with next file
            }
        }
        
        return anyExtracted;
    }
    
    private void deleteDirectory(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            dir.delete();
        }
    }
    
    /**
     * Downloads the new launcher JAR and replaces the current one.
     * Returns true if successful.
     */
    private boolean downloadAndReplaceLauncher() {
        try {
            // Download to temp location
            Path tempLauncher = Paths.get(GAME_DIRECTORY, "launcher_update.jar");
            if (!downloadFile(REMOTE_BASE_URL + "RuneCartel-Launcher.jar", tempLauncher)) {
                System.out.println("Failed to download launcher update");
                return false;
            }
            
            // Get current launcher JAR path
            String currentJarPath = getCurrentJarPath();
            if (currentJarPath == null) {
                System.out.println("Could not determine current launcher path");
                return false;
            }
            
            // Create update script that will replace the JAR after we exit
            createUpdateScript(tempLauncher.toString(), currentJarPath);
            
            return true;
        } catch (Exception e) {
            System.out.println("Launcher update error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Gets the path of the currently running JAR file.
     */
    private String getCurrentJarPath() {
        try {
            return new File(Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Creates a script to replace the launcher and runs it, then restarts.
     */
    private void createUpdateScript(String newLauncherPath, String currentLauncherPath) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            File scriptFile;
            ProcessBuilder pb;
            
            if (os.contains("win")) {
                // Windows batch script
                scriptFile = new File(GAME_DIRECTORY + File.separator + "update_launcher.bat");
                String script = "@echo off\r\n" +
                    "timeout /t 2 /nobreak > nul\r\n" +
                    "copy /Y \"" + newLauncherPath + "\" \"" + currentLauncherPath + "\"\r\n" +
                    "del \"" + newLauncherPath + "\"\r\n" +
                    "start \"\" \"" + currentLauncherPath + "\"\r\n" +
                    "del \"%~f0\"\r\n";
                Files.write(scriptFile.toPath(), script.getBytes(StandardCharsets.UTF_8));
                
                pb = new ProcessBuilder("cmd", "/c", scriptFile.getAbsolutePath());
            } else {
                // Unix shell script
                scriptFile = new File(GAME_DIRECTORY + File.separator + "update_launcher.sh");
                String script = "#!/bin/bash\n" +
                    "sleep 2\n" +
                    "cp -f \"" + newLauncherPath + "\" \"" + currentLauncherPath + "\"\n" +
                    "rm \"" + newLauncherPath + "\"\n" +
                    "java -jar \"" + currentLauncherPath + "\" &\n" +
                    "rm \"$0\"\n";
                Files.write(scriptFile.toPath(), script.getBytes(StandardCharsets.UTF_8));
                scriptFile.setExecutable(true);
                
                pb = new ProcessBuilder("bash", scriptFile.getAbsolutePath());
            }
            
            pb.directory(new File(GAME_DIRECTORY));
            pb.start();
        } catch (Exception e) {
            System.out.println("Could not create update script: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Restarts the launcher by exiting - the update script will handle the restart.
     */
    private void restartLauncher() {
        System.exit(0);
    }
    
    private void saveVersionInfo(int clientVersion, int cacheVersion) {
        try {
            Properties props = new Properties();
            props.setProperty("client.version", String.valueOf(clientVersion));
            props.setProperty("cache.version", String.valueOf(cacheVersion));
            props.setProperty("launcher.version", String.valueOf(LAUNCHER_VERSION));
            
            File versionFile = new File(GAME_DIRECTORY + File.separator + VERSION_FILE);
            try (FileOutputStream fos = new FileOutputStream(versionFile)) {
                props.store(fos, "RuneCartel's HQ Version Info");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void launchGame() {
        try {
            String clientPath = GAME_DIRECTORY + File.separator + CLIENT_JAR;
            String javaExecutable = resolveJavaExecutable();
            File clientOutput = new File(GAME_DIRECTORY + File.separator + "client-output.txt");
            resetClientOutput(clientOutput);

            List<LaunchProfile> launchProfiles = buildLaunchProfiles(javaExecutable);
            for (int attempt = 0; attempt < launchProfiles.size(); attempt++) {
                LaunchProfile launchProfile = launchProfiles.get(attempt);
                applyRecommendedClientSettings(launchProfile);
                updateStatus("Launching game... (" + launchProfile.maxHeapMb + " MB heap)");
                System.out.println("Launching client with " + launchProfile);

                ProcessBuilder pb = new ProcessBuilder(buildLaunchCommand(javaExecutable, clientPath, launchProfile));
                pb.directory(new File(GAME_DIRECTORY));
                pb.redirectErrorStream(true);
                pb.redirectOutput(ProcessBuilder.Redirect.appendTo(clientOutput));

                Process process = pb.start();

                if (!process.waitFor(4, TimeUnit.SECONDS)) {
                    // Close launcher only after the client stays alive long enough to be considered launched.
                    Thread.sleep(1000);
                    System.exit(0);
                    return;
                }

                int exitCode = process.exitValue();
                boolean canRetry = attempt + 1 < launchProfiles.size();
                if (canRetry && isHeapReservationFailure(clientOutput)) {
                    updateStatus("Retrying with lower memory settings...");
                    System.out.println("Client failed to reserve heap; retrying with a smaller profile.");
                    continue;
                }

                updateStatus("Client failed to launch (exit " + exitCode + "). See client-output.txt");
                return;
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            updateStatus("Error launching: " + e.getMessage());
        }
    }

    private String resolveJavaExecutable() {
        File preferred = new File(PREFERRED_JAVA_PATH);
        if (preferred.exists() && preferred.isFile()) {
            return preferred.getAbsolutePath();
        }
        return "java";
    }

    private List<String> buildLaunchCommand(String javaExecutable, String clientPath, LaunchProfile launchProfile) {
        List<String> command = new ArrayList<>();
        command.add(javaExecutable);
        command.add("-XX:-OmitStackTraceInFastThrow");
        // Java2D hardware acceleration - critical for canvas blit performance
        command.add("-Dsun.java2d.d3d=true");
        command.add("-Dsun.java2d.noddraw=false");
        command.add("-Dsun.java2d.d3dtexbpp=16");
        // Optimize for throughput GC (less GC pauses during rendering)
        command.add("-XX:+UseG1GC");
        command.add("-XX:MaxGCPauseMillis=10");
        command.add("-Xmx" + launchProfile.maxHeapMb + "m");
        command.add("-Xms" + launchProfile.initialHeapMb + "m");
        command.add("-jar");
        command.add(clientPath);
        return command;
    }

    private List<LaunchProfile> buildLaunchProfiles(String javaExecutable) {
        List<LaunchProfile> profiles = new ArrayList<>();
        LaunchProfile primaryProfile = determineLaunchProfile(javaExecutable);
        profiles.add(primaryProfile);

        int[] fallbackHeaps = {1024, 768, 512};
        for (int heapMb : fallbackHeaps) {
            if (heapMb < primaryProfile.maxHeapMb && !containsHeapProfile(profiles, heapMb)) {
                profiles.add(createLaunchProfile(heapMb));
            }
        }

        if (profiles.isEmpty()) {
            profiles.add(createLaunchProfile(512));
        }
        return profiles;
    }

    private boolean containsHeapProfile(List<LaunchProfile> profiles, int heapMb) {
        for (LaunchProfile profile : profiles) {
            if (profile.maxHeapMb == heapMb) {
                return true;
            }
        }
        return false;
    }

    private LaunchProfile determineLaunchProfile(String javaExecutable) {
        boolean is64Bit = isLikely64BitJava(javaExecutable);

        int heapMb;
        if (!is64Bit) {
            heapMb = 768;
        } else {
            // Always try 1536MB for 64-bit systems. The fallback mechanism in
            // buildLaunchProfiles() will step down to 1024 → 768 → 512 if the
            // OS genuinely cannot reserve this much.
            heapMb = 1536;
        }

        return createLaunchProfile(heapMb);
    }

    private LaunchProfile createLaunchProfile(int heapMb) {
        int initialHeapMb = Math.min(512, Math.max(256, heapMb / 2));
        int modelCacheMb = heapMb >= 1536 ? 512 : heapMb >= 1024 ? 384 : heapMb >= 768 ? 256 : 192;
        int gpuDrawDistance = heapMb >= 1536 ? 18 : heapMb >= 1024 ? 16 : 12;
        int hdDrawDistance = heapMb >= 1536 ? 22 : heapMb >= 1024 ? 18 : 14;
        return new LaunchProfile(heapMb, initialHeapMb, modelCacheMb, gpuDrawDistance, hdDrawDistance);
    }

    private boolean isLikely64BitJava(String javaExecutable) {
        String lowerJavaPath = javaExecutable.toLowerCase();
        if (lowerJavaPath.contains("program files (x86)")) {
            return false;
        }
        // If the executable is under "Program Files" (not x86), it's almost certainly 64-bit.
        if (lowerJavaPath.contains("program files")) {
            return true;
        }
        // Probe the target JVM instead of relying on the launcher's own JVM properties.
        try {
            ProcessBuilder pb = new ProcessBuilder(javaExecutable, "-XshowSettings:vm", "-version");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            byte[] output = process.getInputStream().readAllBytes();
            process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            String versionOutput = new String(output, java.nio.charset.StandardCharsets.UTF_8);
            if (versionOutput.contains("64-Bit") || versionOutput.contains("64-bit")) {
                return true;
            }
            if (versionOutput.contains("Client VM")) {
                return false; // 32-bit Client VM
            }
        } catch (Exception e) {
            System.out.println("Could not probe java executable for architecture: " + e.getMessage());
        }
        // Fallback to launcher JVM properties
        return System.getProperty("os.arch", "").contains("64") || System.getProperty("sun.arch.data.model", "").contains("64");
    }

    private long getFreePhysicalMemoryMb() {
        try {
            java.lang.management.OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
            if (operatingSystemMXBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunBean = (com.sun.management.OperatingSystemMXBean) operatingSystemMXBean;
                return sunBean.getFreePhysicalMemorySize() / (1024L * 1024L);
            }
        } catch (Exception e) {
            System.out.println("Could not detect free physical memory: " + e.getMessage());
        }
        return -1L;
    }

    private void resetClientOutput(File clientOutput) {
        try {
            Files.write(clientOutput.toPath(), new byte[0], StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.out.println("Could not reset client-output.txt: " + e.getMessage());
        }
    }

    private boolean isHeapReservationFailure(File clientOutput) {
        try {
            if (!clientOutput.exists()) {
                return false;
            }

            byte[] outputBytes = Files.readAllBytes(clientOutput.toPath());
            String output = new String(outputBytes, StandardCharsets.UTF_8);
            return output.contains("Could not reserve enough space") || output.contains("Error occurred during initialization of VM");
        } catch (IOException e) {
            System.out.println("Could not inspect client-output.txt: " + e.getMessage());
            return false;
        }
    }

    private void applyRecommendedClientSettings(LaunchProfile launchProfile) {
        File settingsFile = new File(GAME_DIRECTORY + File.separator + "settings.properties");
        Properties props = new Properties();

        if (settingsFile.exists()) {
            try (FileInputStream fis = new FileInputStream(settingsFile)) {
                props.load(fis);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        boolean legacyForcedSoftwareProfile = "false".equalsIgnoreCase(props.getProperty("runelite.hdplugin"))
            && "false".equalsIgnoreCase(props.getProperty("runelite.gpuplugin"));

        if (legacyForcedSoftwareProfile) {
            // Migrate older launcher installs out of the forced software profile.
            props.setProperty("runelite.hdplugin", "true");
            props.setProperty("runelite.gpuplugin", "false");
            props.setProperty("hd.shadowMode", "OFF");
            props.setProperty("hd.antiAliasingMode", "DISABLED");
            props.setProperty("hd.maxDynamicLights", "NONE");
            props.setProperty("hd.normalMapping", "true");
            props.setProperty("hd.parallaxOcclusionMappingToggle", "false");
            props.setProperty("hd.environmentalLighting", "true");
            props.setProperty("hd.projectileLights", "true");
            props.setProperty("hd.npcLights", "true");
            props.setProperty("hd.groundFog", "false");
            props.setProperty("hd.underwaterCaustics", "true");
        }

        props.setProperty("runelite.hdplugin", "true");
        props.setProperty("runelite.gpuplugin", "false");

        props.setProperty("gpu.useComputeShaders", "false");
        props.setProperty("gpu.drawDistance", String.valueOf(launchProfile.gpuDrawDistance));
        props.setProperty("gpu.antiAliasingMode", "DISABLED");
        props.setProperty("gpu.anisotropicFilteringLevel", "4");
        props.setProperty("gpu.vsyncMode", "OFF");
        props.setProperty("gpu.unlockFps", "true");
        props.setProperty("gpu.fpsTarget", "90");

        props.setProperty("fpscontrol.limitFps", "false");
        props.setProperty("fpscontrol.maxFps", "90");
        props.setProperty("fpscontrol.drawFps", "false");
        props.setProperty("stretchedmode.increasedPerformance", "true");
        props.setProperty("animationSmoothing.smoothPlayerAnimations", "false");
        props.setProperty("animationSmoothing.smoothNpcAnimations", "false");
        props.setProperty("animationSmoothing.smoothGraphicAnimations", "false");
        props.setProperty("animationSmoothing.smoothObjectAnimations", "false");

        props.setProperty("hd.drawDistance", String.valueOf(launchProfile.hdDrawDistance));
        props.setProperty("hd.shadowMode", "OFF");
        props.setProperty("hd.shadowResolution", "RES_1024");
        props.setProperty("hd.shadowDistance", "DISTANCE_20");
        props.setProperty("hd.antiAliasingMode", "DISABLED");
        props.setProperty("hd.anisotropicFilteringLevel", "4");
        props.setProperty("hd.unlockFps", "true");
        props.setProperty("hd.vsyncMode", "OFF");
        props.setProperty("hd.fpsTarget", "90");
        props.setProperty("hd.maxDynamicLights", "NONE");
        props.setProperty("hd.normalMapping", "true");
        props.setProperty("hd.parallaxOcclusionMappingToggle", "false");
        props.setProperty("hd.environmentalLighting", "true");
        props.setProperty("hd.projectileLights", "true");
        props.setProperty("hd.npcLights", "true");
        props.setProperty("hd.groundFog", "false");
        props.setProperty("hd.underwaterCaustics", "true");
        props.setProperty("hd.objectTextures", "true");
        props.setProperty("hd.groundTextures", "true");
        props.setProperty("hd.textureResolution", "RES_256");
        props.setProperty("hd.hdInfernalTexture", "true");
        props.setProperty("hd.useModelCaching", "true");
        props.setProperty("hd.modelCacheSizeMiB", String.valueOf(launchProfile.modelCacheMb * 4));
        props.setProperty("hd.modelCacheSizeMiBv2", String.valueOf(launchProfile.modelCacheMb));

        props.setProperty("entityhider.hidePlayers", "false");
        props.setProperty("entityhider.hidePlayers2D", "false");

        try (FileOutputStream fos = new FileOutputStream(settingsFile)) {
            props.store(fos, "RuneLite configuration");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setIfMissing(Properties props, String key, String value) {
        if (!props.containsKey(key)) {
            props.setProperty(key, value);
        }
    }

    private void updateStatus(String message) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(message));
    }
    
    private void setProgress(int value) {
        SwingUtilities.invokeLater(() -> progressBar.setValue(value));
    }

    private static final class LaunchProfile {
        private final int maxHeapMb;
        private final int initialHeapMb;
        private final int modelCacheMb;
        private final int gpuDrawDistance;
        private final int hdDrawDistance;

        private LaunchProfile(int maxHeapMb, int initialHeapMb, int modelCacheMb, int gpuDrawDistance, int hdDrawDistance) {
            this.maxHeapMb = maxHeapMb;
            this.initialHeapMb = initialHeapMb;
            this.modelCacheMb = modelCacheMb;
            this.gpuDrawDistance = gpuDrawDistance;
            this.hdDrawDistance = hdDrawDistance;
        }

        @Override
        public String toString() {
            return "Xmx=" + maxHeapMb + "m, Xms=" + initialHeapMb + "m, modelCache=" + modelCacheMb + "m, gpuDrawDistance=" + gpuDrawDistance + ", hdDrawDistance=" + hdDrawDistance;
        }
    }

    private static EmbeddedVersions loadEmbeddedVersions() {
        Properties props = new Properties();
        try (InputStream is = Launcher.class.getResourceAsStream(EMBEDDED_VERSION_FILE)) {
            if (is != null) {
                props.load(is);
            }
        } catch (Exception e) {
            System.out.println("Could not load embedded version.properties: " + e.getMessage());
        }

        return new EmbeddedVersions(
                parseEmbeddedVersion(props, "client.version", 11),
                parseEmbeddedVersion(props, "cache.version", 4),
                parseEmbeddedVersion(props, "launcher.version", 1)
        );
    }

    private static int parseEmbeddedVersion(Properties props, String key, int fallback) {
        try {
            return Integer.parseInt(props.getProperty(key, String.valueOf(fallback)));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static final class EmbeddedVersions {
        private final int clientVersion;
        private final int cacheVersion;
        private final int launcherVersion;

        private EmbeddedVersions(int clientVersion, int cacheVersion, int launcherVersion) {
            this.clientVersion = clientVersion;
            this.cacheVersion = cacheVersion;
            this.launcherVersion = launcherVersion;
        }
    }
    
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Use default
        }
        
        SwingUtilities.invokeLater(() -> {
            Launcher launcher = new Launcher();
            launcher.setVisible(true);
        });
    }
}

