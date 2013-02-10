package com.wadpam.rnr.dao;

import com.wadpam.rnr.domain.DLike;

import java.util.List;

/**
 * Business Methods interface for entity DLike.
 * This interface is generated by mardao, but edited by developers.
 * It is not overwritten by the generator once it exists.
 *
 * Generated on 2012-08-05T20:54:54.772+0700.
 * @author mardao DAO generator (net.sf.mardao.plugin.ProcessDomainMojo)
 */
public interface DLikeDao extends GeneratedDLikeDao {

    /**
     * Find likes done by a specific user and a specific product.
     * @param productId the product
     * @param username the unique user name or id
     * @return a like
     */
    public DLike findByProductIdUsername(String productId, String username);

    /**
     * Return a random set on Likes for the product id
     * @param productId the product id
     * @param limit the number of random likes to return
     * @return iterable likes
     */
    public List<DLike> findRandomByProductId(String productId, int limit);
}
