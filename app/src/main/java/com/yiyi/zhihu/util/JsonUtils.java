package com.yiyi.zhihu.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;

import android.util.Log;

import java.lang.ref.SoftReference;
import java.lang.reflect.Type;

/**
 *
 */
@SuppressWarnings("unused")
public final class JsonUtils
{
    private static SoftReference<Gson> mGson;

    public static String convertUnicode(String ori)
    {
        char aChar;
        int len = ori.length();
        StringBuffer outBuffer = new StringBuffer(len);
        for (int x = 0; x < len; )
        {
            aChar = ori.charAt(x++);
            if (aChar == '\\')
            {
                aChar = ori.charAt(x++);
                if (aChar == 'u')
                {
                    // Read the xxxx
                    int value = 0;
                    for (int i = 0; i < 4; i++)
                    {
                        aChar = ori.charAt(x++);
                        switch (aChar)
                        {
                            case '0':
                            case '1':
                            case '2':
                            case '3':
                            case '4':
                            case '5':
                            case '6':
                            case '7':
                            case '8':
                            case '9':
                                value = (value << 4) + aChar - '0';
                                break;
                            case 'a':
                            case 'b':
                            case 'c':
                            case 'd':
                            case 'e':
                            case 'f':
                                value = (value << 4) + 10 + aChar - 'a';
                                break;
                            case 'A':
                            case 'B':
                            case 'C':
                            case 'D':
                            case 'E':
                            case 'F':
                                value = (value << 4) + 10 + aChar - 'A';
                                break;
                            default:
                                throw new IllegalArgumentException(
                                        "Malformed   \\uxxxx   encoding.");
                        }
                    }
                    outBuffer.append((char) value);
                }
                else
                {
                    if (aChar == 't')
                        aChar = '\t';
                    else if (aChar == 'r')
                        aChar = '\r';
                    else if (aChar == 'n')
                        aChar = '\n';
                    else if (aChar == 'f')
                        aChar = '\f';
                    outBuffer.append(aChar);
                }
            }
            else
                outBuffer.append(aChar);

        }
        return outBuffer.toString();
    }

    public static String format(String jsonStr)
    {
        int level = 0;
        StringBuffer jsonForMatStr = new StringBuffer();
        for (int i = 0; i < jsonStr.length(); i++)
        {
            char c = jsonStr.charAt(i);
            if (level > 0 && '\n' == jsonForMatStr.charAt(jsonForMatStr.length() - 1))
            {
                jsonForMatStr.append(getLevelStr(level));
            }
            switch (c)
            {
                case '{':
                case '[':
                    jsonForMatStr.append(c + "\n");
                    level++;
                    break;
                case ',':
                    jsonForMatStr.append(c + "\n");
                    break;
                case '}':
                case ']':
                    jsonForMatStr.append("\n");
                    level--;
                    jsonForMatStr.append(getLevelStr(level));
                    jsonForMatStr.append(c);
                    break;
                default:
                    jsonForMatStr.append(c);
                    break;
            }
        }

        return jsonForMatStr.toString();

    }

    /**
     * 支持将JSONObject里面的每个属性,映射到Entity的属性,从而得到对应的Entity实例.
     */
    //    public static <T extends BaseEntity> T fromJSONObject(final JSONObject jsonObject, final Class<T> classOfT) throws JsonSyntaxException, JSONException, IllegalAccessException, InstantiationException
    //    {
    //        final ContentValues contentValues = new ContentValues();
    //        final Iterator<?> keys = jsonObject.keys();
    //        while (keys.hasNext())
    //        {
    //            final String key = TextUtils.valueOfNoNull(keys.next());
    //            if (TextUtils.isEmpty(key) || jsonObject.isNull(key))
    //                continue;
    //            final String key_value = TextUtils.valueOfNoNull(jsonObject.get(key));
    //
    //            contentValues.put(key, key_value);
    //        }
    //        T instance = classOfT.newInstance();
    //        instance.setValues(contentValues, false);
    //        return instance;
    //    }
    public static <T> T fromJson(final String json, final Type typeOfT) throws JsonSyntaxException
    {
        return getGson().fromJson(json, typeOfT);
    }

    public static <T> T fromJson(final String json, final Class<T> classOfT) throws JsonSyntaxException
    {
        try
        {
            return getGson().fromJson(json, classOfT);
        }
        catch (final IllegalStateException e)
        {
            //后台捕获到此错误量较少，可以以Warning级别记录到本地日志文件
            Log.w("JsonUtils.fromJson", json);

            //但是不能以Error级别上传到友盟，因为Json可能很大！
            Log.e("JsonUtils.fromJson", "解析出错的JSON已经记录到本地日志", e);
            throw e;
        }
    }

    private static Gson getGson()
    {
        if (mGson == null || mGson.get() == null)
            mGson = new SoftReference<>(new Gson());
        return mGson.get();
    }

    private static String getLevelStr(int level)
    {
        StringBuffer levelStr = new StringBuffer();
        for (int levelI = 0; levelI < level; levelI++)
        {
            levelStr.append("\t");
        }
        return levelStr.toString();
    }

    public static String toJson(final Object src)
    {
        return getGson().toJson(src);
    }

    public static JsonElement toJsonTree(final Object src)
    {
        return getGson().toJsonTree(src);
    }
}