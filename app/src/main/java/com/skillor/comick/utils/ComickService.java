package com.skillor.comick.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.json.JSONArray;
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
import java.util.List;

public class ComickService {
    private static ComickService INSTANCE;

    private final MutableLiveData<String> errorText = new MutableLiveData<>();
    private final List<Comic> comicList = new ArrayList<>();
    private final MutableLiveData<List<Comic>> comics = new MutableLiveData<>();

    private File directory;

    private final static String BASE_URL = "https://comick.fun/";
    private final static String COMIC_SUFFIX = ".comic";

    private ComickService() {
        comics.setValue(new ArrayList<>());
    }

    public void initialize(File directory) {
        this.directory = directory;
        for (File file : this.directory.listFiles()) {
            if (file.isDirectory() && file.getPath().endsWith(COMIC_SUFFIX)) {
                try {
                    comicList.add(new Comic(file, directory));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        comics.postValue(comicList);
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

    public void downloadComic(String url) {
        errorText.postValue("");
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    Comic c = new Comic(url);
                    c.saveInfo(directory);
                    c.downloadImage(directory);
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

    public class Comic {
        private final static String INFO_FILE = "info.json";

        private String comicTitle;
        private String comicPath;
        private String comicDataBase;
        private double firstChapterI;
        private double lastChapterI;
        private String firstChapterId;
        private String lastChapterId;
        private String imageUrl;
        private Bitmap coverBitmap;

        public String getComicTitle() {
            return comicTitle;
        }

        public Bitmap getCoverBitmap() {
            return coverBitmap;
        }

        private Comic(String url) throws Exception {
            String comicData = request(url);
            String[] split = comicData.split("/_buildManifest.js\"")[0].split("/");
            String contentId = split[split.length - 1];
            int lastSlash = url.lastIndexOf("/");
            comicPath = url.substring(BASE_URL.length(), lastSlash); // e.g. comic/solo-leveling
            String chapterPath = url.substring(lastSlash); // e.g. nOrQY-chapter-0-en
            comicDataBase = BASE_URL + "_next/data/" + contentId + "/" + comicPath + "/";

            JSONObject chapter = new JSONObject(request(comicDataBase + chapterPath + ".json"));

            JSONObject comicInfo = chapter.getJSONObject("pageProps").getJSONObject("chapter").getJSONObject("md_comics");
            comicTitle = comicInfo.getString("title");
            imageUrl = comicInfo.getJSONArray("md_covers").getJSONObject(0).getString("gpurl");

            JSONArray chapters = chapter.getJSONObject("pageProps").getJSONArray("chapters");
            JSONObject firstChapter = chapters.getJSONObject(0);
            firstChapterI = Double.parseDouble(firstChapter.getString("chap"));
            JSONObject lastChapter = firstChapter;
            lastChapterI = firstChapterI;
            for (int i=0; i < chapters.length(); i++) {
                JSONObject currentChapter = chapters.getJSONObject(i);
                if (Double.parseDouble(currentChapter.getString("chap")) < firstChapterI) {
                    firstChapter = currentChapter;
                    firstChapterI = Double.parseDouble(currentChapter.getString("chap"));
                } else if (Double.parseDouble(currentChapter.getString("chap")) > lastChapterI) {
                    lastChapter = currentChapter;
                    lastChapterI = Double.parseDouble(currentChapter.getString("chap"));
                }
            }

            firstChapterId = firstChapter.getString("hid") + "-chapter-" + firstChapter.getString("chap") + "-" + firstChapter.getString("lang");
            lastChapterId = lastChapter.getString("hid") + "-chapter-" + lastChapter.getString("chap") + "-" + lastChapter.getString("lang");
        }

        private Comic(File file, File directory) throws Exception {
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

            loadImage(directory);
        }

        public String getDirectoryPath(File directory) {
            return directory.getAbsolutePath() + File.separator + comicTitle + COMIC_SUFFIX;
        }

        public String getInfoPath(File directory) {
            return getDirectoryPath(directory) + File.separator + INFO_FILE;
        }

        public String getCoverPath(File directory) {
            return getDirectoryPath(directory) + File.separator + "cover.jpg";
        }

        public void saveInfo(File directory) throws Exception {
            File file = new File(getInfoPath(directory));
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

        public void downloadImage(File directory) throws Exception {
            File file = new File(getCoverPath(directory));
            file.getParentFile().mkdirs();
            URL url = new URL(imageUrl);
            InputStream in = new BufferedInputStream(url.openStream());
            OutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int n = 0;
            while (-1!=(n=in.read(buf))) {
                out.write(buf, 0, n);
            }
            out.close();
            in.close();
            loadImage(directory);
        }

        private void loadImage(File directory) throws FileNotFoundException {
            coverBitmap = BitmapFactory.decodeStream(new FileInputStream(getCoverPath(directory)));
        }
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
