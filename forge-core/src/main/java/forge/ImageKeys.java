package forge;

import forge.card.CardEdition;
import forge.item.PaperCard;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class ImageKeys {
    public static final String CARD_PREFIX           = "c:";
    public static final String TOKEN_PREFIX          = "t:";
    public static final String ICON_PREFIX           = "i:";
    public static final String BOOSTER_PREFIX        = "b:";
    public static final String FATPACK_PREFIX        = "f:";
    public static final String BOOSTERBOX_PREFIX     = "x:";
    public static final String PRECON_PREFIX         = "p:";
    public static final String TOURNAMENTPACK_PREFIX = "o:";
    public static final String ADVENTURECARD_PREFIX = "a:";

    public static final String HIDDEN_CARD           = "hidden";
    public static final String MORPH_IMAGE           = "morph";
    public static final String DISGUISED_IMAGE       = "disguised";
    public static final String MANIFEST_IMAGE        = "manifest";
    public static final String CLOAKED_IMAGE         = "cloaked";
    public static final String FORETELL_IMAGE        = "foretell";

    public static final String BACKFACE_POSTFIX  = "$alt";
    public static final String SPECFACE_W = "$wspec";
    public static final String SPECFACE_U = "$uspec";
    public static final String SPECFACE_B = "$bspec";
    public static final String SPECFACE_R = "$rspec";
    public static final String SPECFACE_G = "$gspec";

    public static final String DEFAULT_2015 = "_2015/";
    public static final String DEFAULT_2003 = "_2003/";
    public static final String DEFAULT_1997 = "_1997/";
    public static final String DEFAULT_1993 = "_1993/";
    public static final LocalDate D2015 = LocalDate.parse("2014-07-17", DateTimeFormatter.ISO_LOCAL_DATE);
    public static final LocalDate D2003 = LocalDate.parse("2003-07-27", DateTimeFormatter.ISO_LOCAL_DATE);
    public static final LocalDate D1997 = LocalDate.parse("1996-10-07", DateTimeFormatter.ISO_LOCAL_DATE);

    public static String ADVENTURE_CARD_PICS_DIR,CACHE_CARD_PICS_DIR,CACHE_TOKEN_PICS_DIR;

    private static final Map<String, String> prefixDirLookup = new HashMap<>();

    private static final Map<String, Set<String>> editionAlias = new HashMap<>();

    private static final Map<String, List<String>> editionDefaultDir = new HashMap<>();

    private static final Map<String, File> cachedCards = new HashMap<>(1000);

    //shortcut for determining if a card image exists for a given card
    //should only be called from PaperCard.hasImage()
    private static final HashMap<String, Set<String>> cachedContent = new HashMap<>(600);

    /**
     * Private constructor to prevent instantiation.
     */
    private ImageKeys() {
    }

    public static void setIsLibGDXPort(boolean value) {}

    public static void initializeDirs(String cards, Map<String, String> cardsSub, String tokens, String icons, String boosters,
            String fatPacks, String boosterBoxes, String precons, String tournamentPacks) {
        CACHE_CARD_PICS_DIR = cards;
        CACHE_TOKEN_PICS_DIR = tokens;
        prefixDirLookup.put(CARD_PREFIX,cards);
        prefixDirLookup.put(TOKEN_PREFIX,tokens);
        prefixDirLookup.put(ICON_PREFIX,icons);
        prefixDirLookup.put(BOOSTER_PREFIX,boosters);
        prefixDirLookup.put(FATPACK_PREFIX,fatPacks);
        prefixDirLookup.put(BOOSTERBOX_PREFIX,boosterBoxes);
        prefixDirLookup.put(PRECON_PREFIX,precons);
        prefixDirLookup.put(TOURNAMENTPACK_PREFIX,tournamentPacks);
        prefixDirLookup.put(ADVENTURECARD_PREFIX,ADVENTURE_CARD_PICS_DIR);
    }

    public static String getTokenKey(String tokenName) {
        return ImageKeys.TOKEN_PREFIX + tokenName;
    }

    public static String getTokenImageName(String tokenKey) {
        if (!tokenKey.startsWith(ImageKeys.TOKEN_PREFIX)) {
            return null;
        }
        return tokenKey.substring(ImageKeys.TOKEN_PREFIX.length());
    }

    public static void clearMissingCards() {}

    public static File getCachedCardsFile(String key) {
        return cachedCards.get(key);
    }

    public static File getImageFile(String key){
        return getImageFile(key,null);
    }

    public static File getImageFile(String key, String pCacheKey) {
        if (StringUtils.isEmpty(key)) return null;
        String[] prefixes = key.split(":");
        final String prefix = prefixes.length == 1?CARD_PREFIX:prefixes[0]+":";
        final String dir = prefixDirLookup.get(prefix);
        final String filename = prefixes.length == 1?key:prefixes[1];
        final String cacheKey = null != pCacheKey?pCacheKey:filename;

        File cachedFile = cachedCards.get(cacheKey);
        if (cachedFile != null) return cachedFile;
        else {
            File file = findFile(dir, filename);
            if (file != null) {
                cachedCards.put(cacheKey, file);
                return file;
            }

            if(!dir.equals(CACHE_TOKEN_PICS_DIR)){
                // if there's an art variant try without it
                String withNoVariant = filename.replaceAll("\\.[a-zA-Z0-9†★]+\\.full$",".full");
                file = findFile(dir, withNoVariant);
                if (file != null) {
                    cachedCards.put(cacheKey, file);
                    return file;
                }

                // if the edition have alias set code
                String setCode = cacheKey.contains("/") ? cacheKey.substring(0, cacheKey.indexOf("/")) : null;
                if (null == pCacheKey && null != setCode && editionAlias.containsKey(setCode)) {
                    for (String alias : editionAlias.get(setCode)) {
                        String aliasFilename =  alias + "/" + filename.split("/")[1];
                        return getImageFile(prefix+aliasFilename,filename);
                    }
                }

                // if there's a SET prefix, try with default dirs
                if (null != setCode && editionDefaultDir.containsKey(setCode)){
                    for(String defaultDir : editionDefaultDir.get(setCode)){
                        file = findFile(dir, defaultDir + withNoVariant.split("/")[1]);
                        if (file != null) {
                            cachedCards.put(cacheKey, file);
                            return file;
                        }
                    }
                }
            }
            else {
                int index = filename.lastIndexOf('_');
                if (index != -1) {
                    String setlessFilename = filename.substring(0, index);

                    // try without set name
                    file = findFile(dir, setlessFilename);
                    if (file != null) {
                        cachedCards.put(filename, file);
                        return file;
                    }
                    // if there's an art variant try without it
                    if (setlessFilename.matches(".*[0-9]*$")) {
                        file = findFile(dir, setlessFilename.replaceAll("[0-9]*$", ""));
                        if (file != null) {
                            cachedCards.put(filename, file);
                            return file;
                        }
                    }
                }
            }
        }

        System.out.println("File not found, no image created: " + key);
        return null;
    }

    public static boolean hasSetLookup(String filename) {
        return false;
    }
    public static File setLookUpFile(String filename, String fullborderFile) {
        return null;
    }
    private static File findFile(String dir, String filename) {
        File f = new File(dir, filename + ".jpg");
        if (f.exists()) {
            return f;
        }
        return null;
    }

    public static boolean hasImage(PaperCard pc) {
        return hasImage(pc, false);
    }

    public static boolean hasImage(PaperCard pc, boolean update) {
        String code = pc.getEdition();

        if (!cachedContent.containsKey(code)) {
            Set<String> setAlias = new HashSet<>();
            Set<String> setFolderContent = new HashSet<>();
            CardEdition ed = StaticData.instance().getEditions().get(code);
            String alias = ed.getAlias();
            String code2 = ed.getCode2();
            String[] files = new File(CACHE_CARD_PICS_DIR + code).list();
            if(files==null || files.length == 0) files = new File(CACHE_CARD_PICS_DIR + alias).list();
            if(files==null || files.length == 0) files = new File(CACHE_CARD_PICS_DIR + code2).list();
            if(files!=null){
                for (String filename : files) {
                    if (!filename.endsWith(".full.jpg")) continue;
                    setFolderContent.add(filename.replace(".full.jpg",""));
                }
            }
            cachedContent.put(code, setFolderContent);

            if (null != alias && alias.length() > 2 && !code.equalsIgnoreCase(alias)) setAlias.add(alias);
            if (null != code2 && code2.length() > 2 && !code.equalsIgnoreCase(code2)) setAlias.add(code2);
            if (!setAlias.isEmpty()) editionAlias.put(code, setAlias);

            LocalDate release = ed.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            if(release.isAfter(D2015)) editionDefaultDir.put(code,Arrays.asList(DEFAULT_2015,DEFAULT_2003,DEFAULT_1997,DEFAULT_1993));
            else if(release.isAfter(D2003)) editionDefaultDir.put(code,Arrays.asList(DEFAULT_2003,DEFAULT_2015,DEFAULT_1997,DEFAULT_1993));
            else if(release.isAfter(D1997)) editionDefaultDir.put(code,Arrays.asList(DEFAULT_1997,DEFAULT_2003,DEFAULT_1993,DEFAULT_2015));
            else editionDefaultDir.put(code,Arrays.asList(DEFAULT_1993,DEFAULT_1997,DEFAULT_2003,DEFAULT_2015));
        }

        String[] keyParts = StringUtils.split(pc.getCardImageKey(), "//");
        if (keyParts.length != 2) return false;

        Set<String> content = cachedContent.get(keyParts[0]);
        return content.contains(keyParts[1].split("\\.")[0]);
    }
}
