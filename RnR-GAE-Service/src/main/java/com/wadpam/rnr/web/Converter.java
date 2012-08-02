package com.wadpam.rnr.web;

import com.google.appengine.api.datastore.GeoPt;
import com.google.appengine.api.datastore.Key;
import com.wadpam.rnr.domain.DComment;
import com.wadpam.rnr.domain.DLike;
import com.wadpam.rnr.domain.DProduct;
import com.wadpam.rnr.domain.DRating;
import com.wadpam.rnr.json.*;
import net.sf.mardao.api.domain.AEDPrimaryKeyEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: mattias
 * Date: 7/28/12
 * Time: 11:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class Converter {

    static final Logger LOG = LoggerFactory.getLogger(Converter.class);


    // Various convert methods for converting between domain to json objects
    protected static JProductV15 convert(DProduct from, HttpServletRequest request) {
        if (null == from) {
            return null;
        }
        final JProductV15 to = new JProductV15();
        convert(from, to);

        to.setId(from.getProductId());
        to.setLocation(convert(from.getLocation()));
        to.setRatingCount(from.getRatingCount());
        to.setRatingSum(from.getRatingSum());
        to.setLikeCount(from.getLikeCount());
        to.setCommentCount(from.getCommentCount());

        // Figure out the base url
        String url = null;
        Pattern pattern = Pattern.compile("^.*/product");
        Matcher matcher = pattern.matcher(request.getRequestURL().toString());
        if (matcher.find()) {
            url = matcher.group() + "/" + from.getProductId();

            // Set links
            to.setRatingsURL(url + "/ratings");
            to.setLikesURL(url + "/likes");
            to.setCommentsURL(url + "/comments");
        }

        return to;
    }

    protected static JRating convert(DRating from, HttpServletRequest request) {
        if (null == from) {
            return null;
        }
        final JRating to = new JRating();
        convert(from, to);

        to.setId(from.getId().toString());
        to.setProductId(from.getProductId());
        to.setUsername(from.getUsername());
        to.setRating(from.getRating().getRating());
        to.setComment(from.getComment());

        return to;
    }

    protected static JLike convert(DLike from, HttpServletRequest request) {
        if (null == from) {
            return null;
        }
        final JLike to = new JLike();
        convert(from, to);

        to.setId(from.getId().toString());
        to.setProductId(from.getProductId());
        to.setUsername(from.getUsername());

        return to;
    }

    protected static JComment convert(DComment from, HttpServletRequest request) {
        if (null == from) {
            return null;
        }
        final JComment to = new JComment();
        convert(from, to);

        to.setId(from.getId().toString());
        to.setProductId(from.getProductId());
        to.setUsername(from.getUsername());
        to.setComment(from.getComment());

        return to;
    }

    protected static JLocation convert(GeoPt from) {
        if (null == from) {
            return null;
        }

        JLocation to = new JLocation(from.getLatitude(), from.getLongitude());

        return to;
    }

    protected static <T extends AEDPrimaryKeyEntity> JBaseObject convert(T from, HttpServletRequest request) {
        if (null == from) {
            return null;
        }

        JBaseObject returnValue;
        if (from instanceof DProduct) {
            returnValue = convert((DProduct) from, request);
        }
        else if (from instanceof DRating) {
            returnValue = convert((DRating) from, request);
        }
        else if (from instanceof DLike) {
            returnValue = convert((DLike) from, request);
        }
        else if (from instanceof DComment) {
            returnValue = convert((DComment) from, request);
        }
        else {
            throw new UnsupportedOperationException("No converter for " + from.getKind());
        }

        return returnValue;
    }

    protected static <T extends AEDPrimaryKeyEntity> Collection<?> convert(Collection<T> from,
                                                                           HttpServletRequest request) {
        if (null == from) {
            return null;
        }

        final Collection<Object> to = new ArrayList<Object>(from.size());

        for(T wf : from) {
            to.add(convert(wf, request));
        }

        return to;
    }

    protected static <T extends AEDPrimaryKeyEntity> void convert(T from, JBaseObject to) {
        if (null == from || null == to) {
            return;
        }

        to.setId(null != from.getSimpleKey() ? from.getSimpleKey().toString() : null);
        to.setCreatedDate(toLong(from.getCreatedDate()));
        to.setUpdatedDate(toLong(from.getUpdatedDate()));
    }

    private static Long toLong(Key from) {
        if (null == from) {
            return null;
        }
        return from.getId();
    }

    private static Long toLong(Date from) {
        if (null == from) {
            return null;
        }
        return from.getTime();
    }

    private static Collection<Long> toLongs(Collection<String> from) {
        if (null == from) {
            return null;
        }

        final Collection<Long> to = new ArrayList<Long>();

        for(String s : from) {
            try {
                to.add(Long.parseLong(s));
            }
            catch (NumberFormatException sometimes) {
                LOG.warn("Trying to convert non-numeric id: {}", s);
            }
        }

        return to;
    }

    private static String toString(Key from) {
        if (null == from) {
            return null;
        }
        return Long.toString(from.getId());
    }

    private static Collection<String> toString(Collection<Long> from) {
        if (null == from) {
            return null;
        }

        final Collection<String> to = new ArrayList<String>(from.size());

        for(Long l : from) {
            to.add(l.toString());
        }

        return to;
    }

}