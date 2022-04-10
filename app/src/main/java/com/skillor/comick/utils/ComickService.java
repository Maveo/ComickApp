package com.skillor.comick.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.skillor.comick.MainActivity;
import com.skillor.comick.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class ComickService {
    private static ComickService INSTANCE;

    private final MutableLiveData<String> errorText = new MutableLiveData<>();
    private final List<Comic> comicList = new ArrayList<>();
    private final MutableLiveData<List<Comic>> comics = new MutableLiveData<>();

    private File directory;
    private MainActivity activity;

    private final static String BASE_URL = "https://comick.fun/";
    private final static String[] BASE_IMAGE_URLS = {"https://meo2.comick.pictures/file/comick/", "https://meo.comick.pictures/"};
    private final static String INFO_FILE = "info.json";
    private final static String COVER_FILE = "cover.jpg";
    private final static String CHAPTERS_DIR = "chapters";
    private final static String COMIC_SUFFIX = ".comic";

    private ComickService() {
        comics.setValue(new ArrayList<>());
    }

    private void initialize() {
        if (directory == null) return;
        comicList.clear();
        for (File file : this.directory.listFiles()) {
            if (file.isDirectory() && file.getPath().endsWith(COMIC_SUFFIX)) {
                try {
                    comicList.add(new Comic(file));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        comics.postValue(comicList);
    }

    public void initialize(MainActivity activity) {
        this.activity = activity;
        this.directory = activity.getApplicationContext().getExternalFilesDir(null);
        initialize();
    }

    public static ComickService getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ComickService();
        }
        return INSTANCE;
    }

    public LiveData<String> getErrorText() {
        return errorText;
    }

    public LiveData<List<Comic>> getComics() {
        return comics;
    }

    public Comic getLastReadComic() {
        if (comicList.isEmpty()) return null;
        String lastRead = activity.getSharedPref().getString(activity.getString(R.string.last_read_key), null);
        if (lastRead != null) {
            return getComicByTitle(lastRead);
        }
        return comicList.get(0);
    }

    public Comic getComicByTitle(String title) {
        for (Comic comic : comicList) {
            if (comic.getComicTitle().equals(title)) {
                return comic;
            }
        }
        return null;
    }

    public File getDirectory() {
        return directory;
    }

    public void downloadComic(String url) {
        errorText.postValue("");
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    Comic c = new Comic(url);
                    for (Comic comic : comicList) {
                        if (comic.getComicTitle().equals(c.getComicTitle())) {
                            errorText.postValue("Comic already exists: " + c.getComicTitle());
                            return;
                        }
                    }
                    c.saveInfo();
                    c.downloadCover();
                    comicList.add(c);
                    comics.postValue(comicList);
                } catch (Exception e) {
                    errorText.postValue(e.getMessage());
                    e.printStackTrace();
                }
            }
        };
        new Thread(runnable).start();
    }

    public void updateComic(int position) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    comicList.get(position).update();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        new Thread(runnable).start();
    }

    public void updateAllComicData() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    for (Comic c : comicList) {
                        c.updateData();
                        c.saveInfo();
                        comics.postValue(comicList);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        new Thread(runnable).start();
    }

    public class Comic {
        private String comicTitle;
        private String comicPath;
        private Double firstChapterI = null;
        private Double lastChapterI = null;
        private Double downloadedFirstChapterI = null;
        private Double downloadedLastChapterI = null;
        private String firstChapterId;
        private String lastChapterId;
        private String[] downloadedChapters = new String[0];
        private final MutableLiveData<String> downloadedLastChapterText = new MutableLiveData<>();
        private Integer currentChapterIndex = null;
        private String imageUrl;
        private Bitmap coverBitmap;
        private boolean isUpdating = false;

        private String prettyPrint(Double d) {
            if (d == null) return activity.getString(R.string.none);
            int i = (int)(double)d;
            return d == i ? String.valueOf(i) : String.valueOf(d);
        }

        public String getComicTitle() {
            return comicTitle;
        }

        public String getComicId() {
            return comicTitle.replaceAll("\\s+", "_").toLowerCase();
        }

        public Bitmap getCoverBitmap() {
            return coverBitmap;
        }

        public double getLastChapterI() {
            return lastChapterI;
        }

        public String getFirstChapterId() {
            return firstChapterId;
        }

        public String getFormattedLastChapterI() {
            return prettyPrint(lastChapterI);
        }

        public Double getDownloadedLastChapterI() {
            return downloadedLastChapterI;
        }

        public String getFormattedDownloadedLastChapterI() {
            return prettyPrint(downloadedLastChapterI);
        }

        public boolean needsUpdate() {
            if (downloadedLastChapterI == null) return true;
            return lastChapterI != (double) downloadedLastChapterI;
        }

        public boolean isUpdating() {
            return isUpdating;
        }

        public LiveData<String> getDownloadedLastChapterText() {
            return downloadedLastChapterText;
        }

        public String getCurrentChapterTitle() {
            if (currentChapterIndex == null) return activity.getString(R.string.none);
            return downloadedChapters[currentChapterIndex];
        }

        public String getCurrentChapterPath() {
            return getChaptersPath() + File.separator + downloadedChapters[currentChapterIndex];
        }

        public String[] getCurrentChapterImages() {
            return (new File(getCurrentChapterPath())).list();
        }

        public Integer getCurrentChapterIndex() {
            return currentChapterIndex;
        }

        public void addChapter(int o) {
            currentChapterIndex += o;
            activity.getSharedPrefEditor().putInt(activity.getString(R.string.current_chapter_prefix_key) + getComicId(), currentChapterIndex);
            activity.getSharedPrefEditor().commit();
        }

        public boolean hasNextChapter() {
            return currentChapterIndex < downloadedChapters.length - 1;
        }

        public boolean hasPrevChapter() {
            return currentChapterIndex > 0;
        }

        private Comic(String url) throws Exception {
            downloadedLastChapterText.postValue(getFormattedDownloadedLastChapterI());

            this.updateData(url);
        }

        public void updateData() throws Exception {
            this.updateData(BASE_URL + comicPath + "/" + firstChapterId);
        }

        public void updateData(String url) throws Exception {
            String comicData = request(url);
            String[] split = comicData.split("/_buildManifest.js\"")[0].split("/");
            String contentId = split[split.length - 1];
            int lastSlash = url.lastIndexOf("/");
            comicPath = url.substring(BASE_URL.length(), lastSlash); // e.g. comic/solo-leveling
            String chapterPath = url.substring(lastSlash); // e.g. nOrQY-chapter-0-en
            String comicDataBase = BASE_URL + "_next/data/" + contentId + "/" + comicPath + "/";

            JSONObject chapter = new JSONObject(request(comicDataBase + chapterPath + ".json"));

            JSONObject comicInfo = chapter.getJSONObject("pageProps").getJSONObject("chapter").getJSONObject("md_comics");
            comicTitle = comicInfo.getString("title");
            imageUrl = comicInfo.getJSONArray("md_covers").getJSONObject(0).getString("gpurl");

            JSONArray chapters = chapter.getJSONObject("pageProps").getJSONArray("chapters");
            JSONObject firstChapter = chapters.getJSONObject(0);
            if (firstChapterI == null) firstChapterI = Double.parseDouble(firstChapter.getString("chap"));
            JSONObject lastChapter = firstChapter;
            if (lastChapterI == null) lastChapterI = firstChapterI;
            for (int i=0; i < chapters.length(); i++) {
                JSONObject currentChapter = chapters.getJSONObject(i);
                double parsed = Double.parseDouble(currentChapter.getString("chap"));
                if (parsed < firstChapterI) {
                    firstChapter = currentChapter;
                    firstChapterI = parsed;
                } else if (parsed > lastChapterI) {
                    lastChapter = currentChapter;
                    lastChapterI = parsed;
                }
            }

            firstChapterId = chapterId(firstChapter);
            lastChapterId = chapterId(lastChapter);
        }

        private Comic(File file) throws Exception {
            InputStream is = new FileInputStream(file.getAbsolutePath() + File.separator + INFO_FILE);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            JSONObject infos = new JSONObject(new String(buffer, StandardCharsets.UTF_8));

            comicTitle = infos.getString("comic_title");
            comicPath = infos.getString("comic_path");
            firstChapterI = infos.getDouble("first_chapter_i");
            lastChapterI = infos.getDouble("last_chapter_i");
            firstChapterId = infos.getString("first_chapter_id");
            lastChapterId = infos.getString("last_chapter_id");

            loadImage();

            downloadedChapters = (new File(getChaptersPath())).list();
            if (downloadedChapters != null) {
                Arrays.sort(downloadedChapters, new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        return Double.valueOf(o1).compareTo(Double.valueOf(o2));
                    }
                });
                currentChapterIndex = activity.getSharedPref().getInt(activity.getString(R.string.current_chapter_prefix_key) + getComicId(), 0);
                if (currentChapterIndex < 0) {
                    currentChapterIndex = 0;
                } else if (currentChapterIndex > downloadedChapters.length - 1) {
                    currentChapterIndex = downloadedChapters.length - 1;
                }
                downloadedFirstChapterI = Double.parseDouble(downloadedChapters[0]);
                downloadedLastChapterI = Double.parseDouble(downloadedChapters[downloadedChapters.length-1]);

            }

            downloadedLastChapterText.postValue(getFormattedDownloadedLastChapterI());
        }

        private String chapterId(JSONObject chapter) throws JSONException {
            return chapter.getString("hid") + "-chapter-" + chapter.getString("chap") + "-" + chapter.getString("lang");
        }

        private String getDirectoryPath() {
            return directory.getAbsolutePath() + File.separator + comicTitle + COMIC_SUFFIX;
        }

        private String getChaptersPath() {
            return getDirectoryPath() + File.separator + CHAPTERS_DIR;
        }

        private String getInfoPath() {
            return getDirectoryPath() + File.separator + INFO_FILE;
        }

        private String getCoverPath() {
            return getDirectoryPath() + File.separator + COVER_FILE;
        }

        public void saveInfo() throws Exception {
            File file = new File(getInfoPath());
            file.getParentFile().mkdirs();

            JSONObject json = new JSONObject();
            json.put("comic_title", comicTitle);
            json.put("comic_path", comicPath);
            json.put("first_chapter_i", firstChapterI);
            json.put("last_chapter_i", lastChapterI);
            json.put("first_chapter_id", firstChapterId);
            json.put("last_chapter_id", lastChapterId);

            Writer writer = new BufferedWriter(new FileWriter(file));
            writer.write(json.toString());
            writer.close();
        }

        public void downloadCover() throws Exception {
            File file = new File(getCoverPath());
            file.getParentFile().mkdirs();
            downloadImage(imageUrl, file);
            loadImage();
        }

        private void loadImage() throws FileNotFoundException {
            coverBitmap = BitmapFactory.decodeStream(new FileInputStream(getCoverPath()));
        }

        private void downloadChapterImage(String imageId, File file) throws Exception {
            for (String baseUrl : BASE_IMAGE_URLS) {
                try {
                    downloadImage(baseUrl + imageId, file);
                    return;
                } catch (Exception ignored) {

                }
            }
            throw new Exception("Image not found: " + imageId);
        }

        public void update() throws Exception {
            isUpdating = true;

            String comicData = request(BASE_URL + comicPath + "/" + firstChapterId);
            String[] split = comicData.split("/_buildManifest.js\"")[0].split("/");
            String contentId = split[split.length - 1];
            String comicDataBase = BASE_URL + "_next/data/" + contentId + "/" + comicPath + "/";

            JSONObject chapter = new JSONObject(request(comicDataBase + firstChapterId + ".json"));

            String currentDownload = firstChapterId;

            if (downloadedLastChapterI != null) {
                JSONArray chapters = chapter.getJSONObject("pageProps").getJSONArray("chapters");
                for (int i=0; i < chapters.length(); i++) {
                    JSONObject currentChapter = chapters.getJSONObject(i);
                    double parsed = Double.parseDouble(currentChapter.getString("chap"));
                    if (parsed == (double)downloadedLastChapterI) {
                        currentDownload = chapterId(chapters.getJSONObject(i));
                    }
                }
            }

            while (true) {
                chapter = new JSONObject(request(comicDataBase + currentDownload + ".json"));
                JSONArray images = chapter.getJSONObject("pageProps").getJSONObject("chapter").getJSONArray("md_images");

                String currentDownloadChapter = chapter.getJSONObject("pageProps").getJSONObject("chapter").getString("chap");

                for (int i=0; i < images.length(); i++) {
                    JSONObject image = images.getJSONObject(i);
                    String imageId = image.getString("b2key");
                    if (!image.isNull("optimized")) {
                        imageId = imageId.substring(0, imageId.lastIndexOf(".")) + "-m.jpg";
                    }
                    File file = new File(getChaptersPath() + File.separator + currentDownloadChapter + File.separator + String.valueOf(i) + ".jpg");
                    file.getParentFile().mkdirs();
                    downloadChapterImage(imageId, file);

                    downloadedLastChapterText.postValue(getFormattedDownloadedLastChapterI() + " - " + String.valueOf((int)((double)i * 100 / images.length())) + "%");
                }

                if (downloadedChapters.length == 0 || !currentDownloadChapter.equals(downloadedChapters[downloadedChapters.length - 1])) {
                    String[] t = new String[downloadedChapters.length + 1];
                    System.arraycopy(downloadedChapters, 0, t, 0, downloadedChapters.length);
                    t[t.length - 1] = currentDownloadChapter;
                    downloadedChapters = t;

                    if (currentChapterIndex == null) {
                        currentChapterIndex = 0;
                    }

                    downloadedLastChapterI = Double.parseDouble(currentDownloadChapter);

                    downloadedLastChapterText.postValue(getFormattedDownloadedLastChapterI());
                }

                if (chapter.getJSONObject("pageProps").isNull("next")) {
                    break;
                }

                currentDownload = chapter.getJSONObject("pageProps").getJSONObject("next").getString("href");
                currentDownload = currentDownload.substring(currentDownload.lastIndexOf("/"));
            }

            this.isUpdating = false;
            downloadedLastChapterText.postValue(getFormattedDownloadedLastChapterI());
        }
    }

    public void downloadImage(String url, File file) throws IOException {
        downloadImage(url, file, 1024);
    }

    public void downloadImage(String url, File file, int bufferSize) throws IOException {
        URL urlO = new URL(url);
        InputStream in = new BufferedInputStream(urlO.openStream());
        OutputStream out = new FileOutputStream(file);
        byte[] buf = new byte[bufferSize];
        int n;
        while (-1!=(n=in.read(buf))) {
            out.write(buf, 0, n);
        }
        out.close();
        in.close();
    }

    public String request(String url) throws IOException {
        return request(url, "GET");
    }

    public String request(String url, String method) throws IOException {
        HttpURLConnection c = null;
        try {
            URL u = new URL(url);
            c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod(method);
            c.setUseCaches(false);
            c.setAllowUserInteraction(false);
            c.connect();
            int status = c.getResponseCode();

            switch (status) {
                case 200:
                case 201:
                    BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line+"\n");
                    }
                    br.close();
                    return sb.toString();
            }
        } finally {
            if (c != null) {
                c.disconnect();
            }
        }
        throw new IOException("Something went wrong while requesting: " + url);
    }
}
