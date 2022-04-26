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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ComickService {
    private static ComickService INSTANCE;

    private final MutableLiveData<Exception> error = new MutableLiveData<>();
    private final Set<Comic> comicSet = new HashSet<>();
    private final MutableLiveData<List<Comic>> comics = new MutableLiveData<>();

    private boolean isOffline = false;

    private File directory;
    private MainActivity activity;

    private final static String BASE_URL = "https://comick.fun/";
    private final static String[] BASE_IMAGE_URLS = {"https://meo2.comick.pictures/file/comick/", "https://meo.comick.pictures/"};
    private final static String INFO_FILE = "info.json";
    private final static String COVER_FILE = "cover.jpg";
    private final static String CHAPTER_DOWNLOAD_FINISHED_FILE = ".finished";
    private final static String CHAPTERS_DIR = "chapters";
    private final static String COMIC_SUFFIX = ".comic";

    public static final int SORTED_AZ_ASC = 1;
    public static final int SORTED_AZ_DESC = 2;
    public static final int SORTED_ADDED_ASC = 3;
    public static final int SORTED_ADDED_DESC = 4;

    private final MutableLiveData<Integer> sorted = new MutableLiveData<>();

    private final DecimalFormatSymbols decimalFormatSymbols;

    private ComickService() {
        comics.setValue(new ArrayList<>());
        sorted.setValue(SORTED_ADDED_DESC);

        decimalFormatSymbols = new DecimalFormatSymbols(Locale.ENGLISH);
        decimalFormatSymbols.setDecimalSeparator('.');
        decimalFormatSymbols.setGroupingSeparator(Character.MIN_VALUE);
    }

    public void initialize() {
        if (directory == null) return;
        comicSet.clear();
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                File[] files = directory.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isDirectory() && file.getPath().endsWith(COMIC_SUFFIX)) {
                            try {
                                comicSet.add(new Comic(file));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                postComics(new ArrayList<>(comicSet));
            }
        });
        t.start();
    }

    public void loadComicByTitle(String comicTitle) {
        if (directory == null) return;
        File file = new File(directory.getAbsolutePath() + File.separator + comicTitle + COMIC_SUFFIX);
        if (!file.isDirectory()) return;
        try {
            comicSet.add(new Comic(file));
        } catch (Exception ignored) {

        }
        postComics(new ArrayList<>(comicSet), true);
    }

    public void setActivity(MainActivity activity) {
        this.activity = activity;
        this.isOffline = activity.getSharedPref().getBoolean(activity.getString(R.string.last_is_offline_key), false);
    }

    public void setDirectory(File directory) {
        this.directory = directory;
    }

    public void setOffline(boolean offline) {
        isOffline = offline;
        activity.getSharedPrefEditor().putBoolean(activity.getString(R.string.last_is_offline_key), isOffline);
        activity.getSharedPrefEditor().commit();
    }

    public boolean isOffline() {
        return isOffline;
    }

    public LiveData<Integer> getSorted() {
        return sorted;
    }

    public int getSortedValue() {
        if (sorted.getValue() == null) {
            return SORTED_ADDED_DESC;
        }
        return sorted.getValue();
    }

    public void setSorted(int sorted) {
        this.sorted.setValue(sorted);
        postComics(new ArrayList<>(comicSet));
    }

    public static ComickService getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ComickService();
        }
        return INSTANCE;
    }

    public String prettyPrint(Double d) {
        if (d == null) return activity.getString(R.string.none);
        return (new DecimalFormat("#.##", decimalFormatSymbols)).format(d);
    }

    public LiveData<Exception> getError() {
        return error;
    }

    private void postComics(List<Comic> comics) {
        postComics(comics, false);
    }

    private void postComics(List<Comic> comics, boolean isSet) {
        Integer sorted = this.sorted.getValue();
        if (sorted != null) {
            Collections.sort(comics, new Comparator<Comic>() {
                @Override
                public int compare(Comic c1, Comic c2) {
                    switch (sorted) {
                        case SORTED_AZ_DESC:
                            return c2.getComicTitle().compareTo(c1.getComicTitle());
                        case SORTED_AZ_ASC:
                            return c1.getComicTitle().compareTo(c2.getComicTitle());
                        case SORTED_ADDED_ASC:
                            return c1.getLastModified().compareTo(c2.getLastModified());
                        default:
                            return c2.getLastModified().compareTo(c1.getLastModified());
                    }
                }
            });
        }
        if (isSet) {
            this.comics.setValue(comics);
        } else {
            this.comics.postValue(comics);
        }
    }

    public LiveData<List<Comic>> getComics() {
        return comics;
    }

    public Comic getLastReadComic() {
        if (comicSet.isEmpty()) return null;
        String lastRead = activity.getSharedPref().getString(activity.getString(R.string.last_read_key), null);
        if (lastRead != null) {
            return getComicByTitle(lastRead);
        }
        return comicSet.toArray(new Comic[0])[0];
    }

    public Comic getComicByTitle(String title) {
        for (Comic comic : comicSet) {
            if (comic.getComicTitle().equals(title)) {
                return comic;
            }
        }
        return null;
    }

    public LiveData<Comic> cacheComic(String url) {
        MutableLiveData<Comic> cachedComic = new MutableLiveData<>();
        cachedComic.setValue(null);
        error.postValue(null);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Comic c = new Comic(url);
                    for (Comic comic : comicSet) {
                        if (comic.getComicTitle().equals(c.getComicTitle())) {
                            error.postValue(new Exception("Comic already exists: " + c.getComicTitle()));
                            return;
                        }
                    }
                    c.cacheCover();
                    cachedComic.postValue(c);
                } catch (Exception e) {
                    error.postValue(e);
                    e.printStackTrace();
                }
            }
        }).start();
        return cachedComic;
    }

    public void addCachedComic(Comic c) {
        comicSet.add(c);
        postComics(new ArrayList<>(comicSet));
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    c.saveInfo();
                    c.downloadCover();
                } catch (Exception e) {
                    error.postValue(e);
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void downloadChapter(Comic.Chapter chapter) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    chapter.download();
                } catch (Exception e) {
                    error.postValue(e);
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void deleteChapter(Comic.Chapter chapter) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    chapter.delete();
                } catch (Exception e) {
                    error.postValue(e);
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public String[][] getChapterImages(Comic.Chapter chapter) {
        final String[][][] value = new String[1][][];
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    value[0] = chapter.getChapterImages();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return value[0];
    }

    public void updateAllComicData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (Comic c : comicSet) {
                        c.updateData();
                        c.saveInfo();
                        postComics(new ArrayList<>(comicSet));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public class Comic {
        public class Chapter {
            private final MutableLiveData<Integer> downloading;
            private final Double chapterI;
            private final String chapterId;
            private boolean abortDownload = false;

            public Chapter(Double chapterI, String chapterId) {
                this.chapterI = chapterI;
                this.chapterId = chapterId;
                if (getFinishedFile().isFile()) {
                    downloading = new MutableLiveData<>(100);
                } else {
                    downloading = new MutableLiveData<>(-1);
                    deleteDirectory(new File(getPath()));
                }
            }

            public void abortDownload() {
                abortDownload = true;
            }

            public String getId() {
                return chapterId;
            }

            public Double getChapterI() {
                return chapterI;
            }

            public boolean isDownloading() {
                Integer d = this.downloading.getValue();
                if (d == null) return false;
                return d >= 0  && d < 100;
            }

            public boolean isDownloaded() {
                Integer d = this.downloading.getValue();
                if (d == null) return false;
                return d == 100;
            }

            public LiveData<Integer> getDownloading() {
                return downloading;
            }

            public String getFormattedI() {
                return prettyPrint(chapterI);
            }

            public String getPath() {
                return getChaptersPath() + File.separator + getFormattedI();
            }

            private File getFinishedFile() {
                return new File(getPath() + File.separator + CHAPTER_DOWNLOAD_FINISHED_FILE);
            }

            public String[][] getChapterImages() throws Exception {
                if (isDownloaded()) {
                    String[] images = (new File(getPath())).list(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String name) {
                            return name.toLowerCase().endsWith(".jpg");
                        }
                    });
                    if (images == null) return new String[0][0];
                    Arrays.sort(images, new Comparator<String>() {
                        @Override
                        public int compare(String o1, String o2) {
                            try {
                                return Integer.compare(Integer.parseInt(o1.substring(0, o1.length() - 4)), Integer.parseInt(o2.substring(0, o2.length() - 4)));
                            } catch (Exception ignored) {
                                return 0;
                            }
                        }
                    });
                    String[][] rimages = new String[images.length][1];
                    for (int i=0; i < images.length; i++) {
                        rimages[i][0] = images[i];
                    }
                    return rimages;
                }
                if (isOffline()) return new String[0][0];

                JSONObject chapter = new JSONObject(request(getComicDataBase() + chapterId + ".json"));

                JSONArray images = chapter.getJSONObject("pageProps").getJSONObject("chapter").getJSONArray("md_images");

                String[][] imageUrls = new String[images.length()][BASE_IMAGE_URLS.length];

                for (int i=0; i < images.length(); i++) {
                    JSONObject image = images.getJSONObject(i);
                    String imageId = image.getString("b2key");
                    if (!image.isNull("optimized")) {
                        imageId = imageId.substring(0, imageId.lastIndexOf(".")) + "-m.jpg";
                    }
                    for (int j=0; j < BASE_IMAGE_URLS.length; j++) {
                        imageUrls[i][j] = BASE_IMAGE_URLS[j] + imageId;
                    }
                }
                return imageUrls;
            }

            private void downloadChapterImage(String imageId, OutputStream out) throws Exception {
                for (String baseUrl : BASE_IMAGE_URLS) {
                    try {
                        downloadImage(baseUrl + imageId, out);
                        return;
                    } catch (Exception ignored) {

                    }
                }
                throw new Exception("Image not found: " + imageId);
            }

            public void download() throws Exception {
                this.downloading.postValue(0);
                JSONObject chapter = new JSONObject(request(getComicDataBase() + chapterId + ".json"));

                JSONArray images = chapter.getJSONObject("pageProps").getJSONObject("chapter").getJSONArray("md_images");

                (new File(getPath())).mkdirs();
                for (int i=0; i < images.length(); i++) {
                    if (abortDownload) {
                        abortDownload = false;
                        this.downloading.postValue(-1);
                        return;
                    }
                    JSONObject image = images.getJSONObject(i);
                    String imageId = image.getString("b2key");
                    if (!image.isNull("optimized")) {
                        imageId = imageId.substring(0, imageId.lastIndexOf(".")) + "-m.jpg";
                    }
                    File file = new File(getPath() + File.separator + i + ".jpg");
                    downloadChapterImage(imageId, new FileOutputStream(file));
                    this.downloading.postValue(i*100/images.length());
                }
                getFinishedFile().createNewFile();
                this.downloading.postValue(100);
            }

            public void delete() {
                deleteDirectory(new File(getPath()));
                this.downloading.postValue(-1);
            }
        }

        private String comicTitle;
        private String comicPath;
        private final long lastModified;
        private Double currentChapterI = null;
        private String coverImageUrl;
        private Bitmap coverBitmap = null;
        private final HashMap<Double, Chapter> chapters = new HashMap<>();
        private String comicDataBase = null;

        public String getComicDataBase() throws Exception {
            if (comicDataBase == null) {
                String comicData = request(BASE_URL + comicPath + "/" + getFirstChapterId());
                String[] split = comicData.split("/_buildManifest.js\"")[0].split("/");
                String contentId = split[split.length - 1];
                comicDataBase = BASE_URL + "_next/data/" + contentId + "/" + comicPath + "/";
            }
            return comicDataBase;
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

        public Double getFirstChapterI() {
            return Collections.min(chapters.keySet());
        }

        public Double getLastChapterI() {
            return Collections.max(chapters.keySet());
        }

        public String getFirstChapterId() {
            Chapter firstChapter = chapters.get(getFirstChapterI());
            if (firstChapter == null) return null;
            return firstChapter.getId();
        }

        public String getFormattedLastChapterI() {
            return prettyPrint(getLastChapterI());
        }

        public Long getLastModified() {
            return lastModified;
        }

        public Chapter getCurrentChapter() {
            return chapters.get(currentChapterI);
        }

        public Double getCurrentChapterI() {
            return currentChapterI;
        }

        public void setCurrentChapterI(double chapter) {
            currentChapterI = chapter;
            activity.getSharedPrefEditor().putString(activity.getString(R.string.current_chapter_prefix_key) + getComicId(), String.valueOf(currentChapterI));
            activity.getSharedPrefEditor().commit();
        }

        public Collection<Chapter> getChapters() {
            return chapters.values();
        }

        public Chapter getChapter(Double key) {
            return chapters.get(key);
        }

        public void nextChapter() {
            double bestHit = getLastChapterI();
            for (Double h: chapters.keySet()) {
                if (h > currentChapterI && h < bestHit) {
                    bestHit = h;
                }
            }
            setCurrentChapterI(bestHit);
        }

        public void prevChapter() {
            double bestHit = getFirstChapterI();
            for (Double h: chapters.keySet()) {
                if (h < currentChapterI && h > bestHit) {
                    bestHit = h;
                }
            }
            setCurrentChapterI(bestHit);
        }

        public boolean hasNextChapter() {
            return currentChapterI < getLastChapterI();
        }

        public boolean hasPrevChapter() {
            return currentChapterI > getFirstChapterI();
        }

        private Comic(String url) throws Exception {
            this.lastModified = System.currentTimeMillis();
            this.updateData(url);
            currentChapterI = getFirstChapterI();
        }

        public void updateData() throws Exception {
            this.updateData(BASE_URL + comicPath + "/" + getFirstChapterId());
        }

        public void updateData(String url) throws Exception {
            String comicData = request(url);
            String[] split = comicData.split("/_buildManifest.js\"")[0].split("/");
            String contentId = split[split.length - 1];
            int lastSlash = url.lastIndexOf("/");
            comicPath = url.substring(BASE_URL.length(), lastSlash); // e.g. comic/solo-leveling
            String chapterPath = url.substring(lastSlash + 1); // e.g. nOrQY-chapter-0-en
            comicDataBase = BASE_URL + "_next/data/" + contentId + "/" + comicPath + "/";

            JSONObject chapter = new JSONObject(request(comicDataBase + chapterPath + ".json"));

            JSONObject comicInfo = chapter.getJSONObject("pageProps").getJSONObject("chapter").getJSONObject("md_comics");
            comicTitle = comicInfo.getString("title");
            coverImageUrl = comicInfo.getJSONArray("md_covers").getJSONObject(0).getString("gpurl");
            if (coverImageUrl.equals("null")) {
                coverImageUrl = comicInfo.getJSONArray("md_covers").getJSONObject(0).getString("b2key");
            }

            JSONArray chapters = chapter.getJSONObject("pageProps").getJSONArray("chapters");
            this.chapters.clear();
            for (int i=0; i < chapters.length(); i++) {
                JSONObject currentChapter = chapters.getJSONObject(i);
                Double ci = Double.parseDouble(currentChapter.getString("chap"));
                this.chapters.put(ci, new Chapter(ci, chapterId(currentChapter)));
            }
        }

        private Comic(File file) throws Exception {
            this.lastModified = file.lastModified();
            InputStream is = new FileInputStream(file.getAbsolutePath() + File.separator + INFO_FILE);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            JSONObject infos = new JSONObject(new String(buffer, StandardCharsets.UTF_8));

            comicTitle = infos.getString("comic_title");
            comicPath = infos.getString("comic_path");
            if (!infos.has("chapters")) {
                updateData(BASE_URL + comicPath + "/" + infos.getString("first_chapter_id"));
                saveInfo();
            } else {
                JSONObject chapters = infos.getJSONObject("chapters");
                for (Iterator<String> it = chapters.keys(); it.hasNext(); ) {
                    String key = it.next();
                    Double ci = Double.parseDouble(key);
                    this.chapters.put(ci, new Chapter(ci, chapters.getString(key)));
                }
            }

            loadImage();

            currentChapterI = Double.parseDouble(activity.getSharedPref().getString(activity.getString(R.string.current_chapter_prefix_key) + getComicId(), "-1"));
            if (currentChapterI < getFirstChapterI()) {
                currentChapterI = getFirstChapterI();
            } else if (currentChapterI > getLastChapterI()) {
                currentChapterI = getLastChapterI();
            }
        }

        @Override
        public int hashCode() {
            return getComicTitle().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (this.getClass() != obj.getClass())
                return false;
            Comic comic = (Comic) obj;
            return getComicTitle().equals(comic.getComicTitle());
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
            JSONObject jChapters = new JSONObject();
            for (Map.Entry<Double, Chapter> entry: chapters.entrySet()) {
                jChapters.put(String.valueOf(entry.getKey()), entry.getValue().getId());
            }
            json.put("chapters", jChapters);

            Writer writer = new BufferedWriter(new FileWriter(file));
            writer.write(json.toString());
            writer.close();
        }

        public void downloadCover() throws Exception {
            File file = new File(getCoverPath());
            file.getParentFile().mkdirs();
            downloadCoverImage(new FileOutputStream(file));
            loadImage();
        }

        public void cacheCover() throws Exception {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            downloadCoverImage(out);
            coverBitmap = BitmapFactory.decodeStream(new ByteArrayInputStream(out.toByteArray()));
        }

        private void downloadCoverImage(OutputStream out) throws Exception {
            try {
                downloadImage(coverImageUrl, out);
                return;
            } catch (Exception ignored) {

            }
            for (String baseUrl : BASE_IMAGE_URLS) {
                try {
                    downloadImage(baseUrl + coverImageUrl, out);
                    return;
                } catch (Exception ignored) {

                }
            }
            throw new Exception("Cover Image not found: " + coverImageUrl);
        }

        private void loadImage() throws FileNotFoundException {
            coverBitmap = BitmapFactory.decodeStream(new FileInputStream(getCoverPath()));
        }
    }

    public void downloadImage(String url, OutputStream out) throws IOException {
        downloadImage(url, out, 1024);
    }

    public void downloadImage(String url, OutputStream out, int bufferSize) throws IOException {
        URL urlO = new URL(url);
        InputStream in = new BufferedInputStream(urlO.openStream());
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
            c.setInstanceFollowRedirects(false);
            c.connect();
            int status = c.getResponseCode();

            switch (status) {
                case 200:
                case 201:
                    BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
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

    boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }
}
