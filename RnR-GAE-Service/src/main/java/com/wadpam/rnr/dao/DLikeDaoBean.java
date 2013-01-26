package com.wadpam.rnr.dao;

import com.wadpam.rnr.domain.DLike;
import net.sf.mardao.core.Filter;

/**
 * Implementation of Business Methods related to entity DLike.
 * This (empty) class is generated by mardao, but edited by developers.
 * It is not overwritten by the generator once it exists.
 *
 * Generated on 2012-08-05T20:54:54.772+0700.
 * @author mardao DAO generator (net.sf.mardao.plugin.ProcessDomainMojo)
 */
public class DLikeDaoBean 
	extends GeneratedDLikeDaoImpl
		implements DLikeDao

{

    @Override
    // Find likes done by user on product
    public DLike findByProductIdUsername(String productId, String username) {
        final Filter filter1 = createEqualsFilter(COLUMN_NAME_PRODUCTID, productId);
        final Filter filter2 = createEqualsFilter(COLUMN_NAME_USERNAME, username);

        return findUniqueBy(filter1, filter2);
    }

}