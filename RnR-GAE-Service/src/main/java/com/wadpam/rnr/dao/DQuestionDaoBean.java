package com.wadpam.rnr.dao;

import com.google.appengine.api.datastore.Query;
import com.wadpam.rnr.domain.DQuestion;
import net.sf.mardao.core.CursorPage;
import net.sf.mardao.core.Filter;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Implementation of Business Methods related to entity DQuestion.
 * This (empty) class is generated by mardao, but edited by developers.
 * It is not overwritten by the generator once it exists.
 *
 * Generated on 2013-02-11T09:17:50.907+0100.
 * @author mardao DAO generator (net.sf.mardao.plugin.ProcessDomainMojo)
 */
public class DQuestionDaoBean 
	extends GeneratedDQuestionDaoImpl
		implements DQuestionDao 
{

    @Override
    public Iterable<DQuestion> queryByTargetUsernameAnswerStateProductId(
            String username,
            Integer answerState,
            String productId) {

        Collection<Filter> filters = new ArrayList<Filter>();

        // User name filter
        filters.add(new Filter(COLUMN_NAME_TAGETUSERNAME, Query.FilterOperator.EQUAL, username));

        // Product filter
        Filter productIdFilter = null;
        if (null != productId) {
            filters.add(new Filter(COLUMN_NAME_PRODUCTID, Query.FilterOperator.EQUAL, productId));
        }

        Filter answerStateFilter = null;
        if (answerState == 1) {
            // Return only questions without any answer
            filters.add(new Filter(COLUMN_NAME_ANSWER, Query.FilterOperator.EQUAL, null));
        } else if (answerState == 2) {
            // Return only question with answer
            filters.add(answerStateFilter = new Filter(COLUMN_NAME_ANSWER, Query.FilterOperator.NOT_EQUAL, null));
        }
        // Otherwise return both, do not set any filter

            Iterable<DQuestion> iterable = queryIterable(false, -1, 1000,
                null, null,
                COLUMN_NAME_ANSWER, true,
                null, false,
                filters.toArray(new Filter[filters.size()]));

        return iterable;
    }


    @Override
    public Iterable<DQuestion> queryByOpUsernameProductId(String opUsername, String productId) {

        Collection<Filter> filters = new ArrayList<Filter>();

        // User name filter
       filters.add(new Filter(COLUMN_NAME_OPUSERNAME, Query.FilterOperator.EQUAL, opUsername));
        // Target user should be null on the original question
       filters.add(new Filter(COLUMN_NAME_TAGETUSERNAME, Query.FilterOperator.EQUAL, null));

        // Product filter
        Filter productIdFilter = null;
        if (null != productId) {
            filters.add(new Filter(COLUMN_NAME_PRODUCTID, Query.FilterOperator.EQUAL, productId));
        }

        Iterable<DQuestion> iterable = queryIterable(false, 0, 1000, null, null,
                COLUMN_NAME_OPUSERNAME, true, null, false,
                filters.toArray(new Filter[filters.size()]));

        return iterable;
    }
}