package com.wadpam.rnr.service;

import com.google.appengine.api.datastore.GeoPt;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.wadpam.open.analytics.google.GoogleAnalyticsTracker;
import com.wadpam.open.exceptions.BadRequestException;
import com.wadpam.open.service.EmailSender;
import com.wadpam.open.transaction.Idempotent;
import com.wadpam.rnr.dao.DAppSettingsDao;
import com.wadpam.rnr.dao.DFeedbackDao;
import com.wadpam.rnr.dao.DFeedbackDaoBean;
import com.wadpam.rnr.dao.DQuestionDao;
import com.wadpam.rnr.domain.DAppSettings;
import com.wadpam.rnr.domain.DFeedback;
import com.wadpam.rnr.domain.DQuestion;
import net.sf.mardao.core.CursorPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * Service for handling user feedback and reporting of inappropriate content.
 * @author mattiaslevin
 */
public class FeedbackService {
    static final Logger LOG = LoggerFactory.getLogger(FeedbackService.class);

    // Offsets for exceptions
    public static final int ERR_BASE = RnrService.ERR_BASE_FEEDBACK;
    public static final int ERR_BASE_FEEDBACK = ERR_BASE + 1000;
    public static final int ERR_BASE_QUESTION = ERR_BASE + 2000;

    private static final int ERR_EMAIL_TO_FROM_MISSING = ERR_BASE + 1;

    // Analytics
    private static final String FEEDBACK_CATEGORY = "Feedback";

    // Optional default email that will be used to send feedback to regardless of domain
    private String toEmail;
    // Will be used as from email, must be set
    private String fromEmail;
    private String fromName;

    // Properties
    private DFeedbackDao feedbackDao;
    private DAppSettingsDao appSettingsDao;
    private DQuestionDao questionDao;

    private boolean tracking = true;

    // Init
    public void init() {
        // Do nothing
    }


    // Feedback methods

    // Add new feedback
    @Idempotent
    @Transactional
    public DFeedback addFeedback(String domain, String title, String feedback, String referenceId,
                                 String category,
                                 String deviceModel, String deviceOS, String deviceOSVersion,
                                 String username, String userContact,
                                 Float latitude, Float longitude,
                                 String toEmail, GoogleAnalyticsTracker tracker) {

        // Get application settings for this domain
        DAppSettings dAppSettings = appSettingsDao.findByPrimaryKey(domain);

        // Store feedback in datastore?
        boolean persist = true;
        if (null != dAppSettings && null != dAppSettings.getPersistFeedback()) {
            persist = dAppSettings.getPersistFeedback();
        }

        DFeedback dFeedback = null;
        if (persist) {

            // Is the user allowed to give feedback more then once in this domain?
            boolean onlyFeedbackOncePerUser = false;
            if (null != dAppSettings && null != dAppSettings.getOnlyFeedbackOncePerUser()) {
                onlyFeedbackOncePerUser = dAppSettings.getOnlyFeedbackOncePerUser();
            }
            if (null != username && onlyFeedbackOncePerUser) {
                // Check if the user already given feedback
                Iterable<DFeedback> dFeedbackIterable = feedbackDao.queryByUsername(username);
                if (null != dFeedbackIterable && dFeedbackIterable.iterator().hasNext()) {
                    dFeedback = dFeedbackIterable.iterator().next();
                }
            }

            if (null == dFeedback) {
                dFeedback = new DFeedback();
            }

            // Set values
            dFeedback.setTitle(title);
            dFeedback.setFeedback(feedback);
            dFeedback.setReferenceId(referenceId);
            dFeedback.setCategory(category);
            dFeedback.setDeviceModel(deviceModel);
            dFeedback.setDeviceOS(deviceOS);
            dFeedback.setDeviceOSVersion(deviceOSVersion);
            dFeedback.setUsername(username);
            dFeedback.setUserContact(userContact);
            if (null != latitude && null != longitude) {
                dFeedback.setLocation(new GeoPt(latitude, longitude));
            } else {
                dFeedback.setLocation(null);
            }

            feedbackDao.persist(dFeedback);
        }

        // Forward in email?
        boolean sendAsEmail = false;
        if (null != dAppSettings && null != dAppSettings.getSendFeedbackAsEmail()) {
            sendAsEmail = dAppSettings.getSendFeedbackAsEmail();
        }

        if (sendAsEmail) {
            // Get destination email address
            String destinationEmail = toEmail;
            if (null == destinationEmail) {
                destinationEmail = this.toEmail;
            }

            if (null != destinationEmail && null != fromEmail) {
                // Build the body
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(feedback).append("\n\n");
                stringBuilder.append("Domain:").append(domain).append("\n");
                stringBuilder.append("Category:").append(category).append("\n");
                stringBuilder.append("Reference Id:").append(referenceId).append("\n");
                stringBuilder.append("User name:").append(username).append("\n");
                stringBuilder.append("User contact:").append(userContact).append("\n");
                stringBuilder.append("Device model:").append(deviceModel).append("\n");
                stringBuilder.append("Device OS:").append(deviceOS).append("\n");
                stringBuilder.append("Device OS version:").append(deviceOSVersion).append("\n");
                stringBuilder.append("Latitude:").append(latitude).append("\n");
                stringBuilder.append("Longitude:").append(longitude).append("\n");

                EmailSender.sendEmail(destinationEmail, fromEmail, title, stringBuilder.toString());
            }
        }

        // Track the event
        if (isTracking() && null != tracker) {
            try {
                tracker.trackEvent(FEEDBACK_CATEGORY, "feedback", referenceId, 1);
            } catch (Exception doNothing) {
                // Make sure this never generates and exception that cause the transaction to fail
                LOG.warn("Sending feedback event to analytics failed:{}", doNothing);
            }
        }

        return dFeedback;
    }

    // Get user feedback
    public DFeedback getFeedback(String domain, Long id) {
        DFeedback dFeedback = feedbackDao.findByPrimaryKey(id);
        return dFeedback;
    }

    // Delete user feedback
    @Idempotent
    @Transactional
    public DFeedback deleteFeedback(String domain, Long id) {
        DFeedback dFeedback = feedbackDao.findByPrimaryKey(id);
        if (null != dFeedback) {
            feedbackDao.delete(dFeedback);
        }
        return dFeedback;
    }

    // Export user feedback newer or equal to the provided timetamp
    public void exportFeedback(String domain, String email, Long timestamp) throws IOException {

        // Check that we have from to and from email address
        if (null == this.fromEmail || null == email) {
            throw new BadRequestException(ERR_EMAIL_TO_FROM_MISSING,
                    String.format("Both to and from email address must be provided when exporting user feedback"));
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        String[] columnNames = {
                DFeedbackDaoBean.COLUMN_NAME_TITLE,
                DFeedbackDaoBean.COLUMN_NAME_FEEDBACK,
                DFeedbackDaoBean.COLUMN_NAME_REFERENCEID,
                DFeedbackDaoBean.COLUMN_NAME_CATEGORY,
                DFeedbackDaoBean.COLUMN_NAME_USERNAME,
                DFeedbackDaoBean.COLUMN_NAME_USERCONTACT,
                DFeedbackDaoBean.COLUMN_NAME_DEVICEMODEL,
                DFeedbackDaoBean.COLUMN_NAME_DEVICEOS,
                DFeedbackDaoBean.COLUMN_NAME_DEVICEOSVERSION,
                DFeedbackDaoBean.COLUMN_NAME_LOCATION,
                DFeedbackDaoBean.COLUMN_NAME_CREATEDDATE
        };

        Iterable<DFeedback> dFeedback = feedbackDao.queryUpdatedAfter(timestamp);
        feedbackDao.writeAsCsv(baos, columnNames, dFeedback);
        baos.flush();
        LOG.info("Wrote CSV file size {} bytes", baos.size());

        // Send email
        EmailSender.sendEmail(this.fromEmail, "Backoffice admin",
                Arrays.asList(email), null, null,
                String.format("User feedback CSV export for %s", domain),
                null, "Please find all registered users in the attached CSV file.\nPlease delete export user feedback to reduce the data stored on the server",
                baos.toByteArray(), "user_feedback_export.csv", "text/csv");

        baos.close();
    }

    // Delete user feedback older the provided timestamp
    @Idempotent
    @Transactional
    public int deleteListOfFeedback(String domain, Long timestamp) {
        int numberDeleted = feedbackDao.deleteAllUpdatedBefore(timestamp);
        return numberDeleted;
    }


    // Report as inappropriate methods

    // Report as inappropriate
    public void reportAsInappropriate(String domain, String referenceId, String referenceDescription,
                                      String username, Float latitude, Float longitude) {

        // TODO

    }

    // Get inappropriate report
    public void getInappropriate(String domain, Long id) {

        // TODO
    }


    // Question

    // Add a question
    public DQuestion addQuestion(String domain, String productId, String opUsername,
                                 String question, List<String> targetUsernames,
                                 GoogleAnalyticsTracker tracker) {
        LOG.debug("Add question for product:{} question:{}", productId, question);

        // Associate the question with the poster
        DQuestion opQuestion = new DQuestion();
        opQuestion.setProductId(productId);
        opQuestion.setOpUsername(opUsername);
        opQuestion.setQuestion(question);
        questionDao.persist(opQuestion);

        // Save one entity for each target user
        Collection<DQuestion> targetQuestions = new ArrayList<DQuestion>(targetUsernames.size());
        for (String targetUsername : targetUsernames) {
            DQuestion targetQuestion = new DQuestion();
            targetQuestion.setParent(questionDao.getPrimaryKey(opQuestion));
            targetQuestion.setProductId(productId);
            targetQuestion.setOpUsername(opUsername);
            targetQuestion.setQuestion(question);
            targetQuestion.setTagetUsername(targetUsername);

            targetQuestions.add(targetQuestion);
        }
        questionDao.persist(targetQuestions);

        // Return the op question
        return opQuestion;
    }

    // Get question by key
    public DQuestion getQuestion(Key key) {
        return questionDao.findByPrimaryKey(key);
    }

    // Get question by key string
    public DQuestion getQuestion(String keyString) {

        // Need to convert the string to a datastore key
        return questionDao.findByPrimaryKey(KeyFactory.stringToKey(keyString));
    }

    // Delete question
    // This method should be used with a key for the original question,
    // it will also delete any created answers. However it will work deleting
    // an answer record only.
    public DQuestion deleteQuestion(Key key) {
        // Putting this code at the top to make the code run faster
        Iterable<Long> iterable = questionDao.queryKeysByParent(key);

        DQuestion dQuestion = questionDao.findByPrimaryKey(key);
        if (null != dQuestion) {
            // Delete parent
            questionDao.delete(dQuestion);

            // Collect all keys to delete
            Collection<Long> keysToDelete = new ArrayList<Long>();
            for (Long id : iterable) {
                keysToDelete.add(id);
            }

            // Delete
            questionDao.delete(key, keysToDelete);
        }

        return dQuestion;
    }


    // Get questions assigned to a specific user
    public Iterable<DQuestion> getQuestionsAssignedToUser(String username, int answerState, String productId) {
        Iterable<DQuestion> iterable =
                questionDao.queryByTargetUsernameAnswerStateProductId(username, answerState, productId);
        return iterable;
    }


    // Get question asked by a specific user
    public Iterable<DQuestion> getQuestionsAskedByUser(String opUsername, String productId) {
        Iterable<DQuestion> iterable =
                questionDao.queryByOpUsernameProductId(opUsername, productId);
        return iterable;
    }


    // Answer a question
    public DQuestion answerQuestion(Key key, long answer) {
        DQuestion dQuestion = questionDao.findByPrimaryKey(key);
        if(null != dQuestion) {
            dQuestion.setAnswer(answer);

            // persist
            questionDao.persist(dQuestion);
        }

        return dQuestion;
    }


    // Get answers for a question
    public Iterable<DQuestion> getAnwsers(Key questionKey) {
        Iterable<DQuestion> iterable = questionDao.queryByParent(questionKey);
        return iterable;
    }


    // Setters and Getters
    public void setAppSettingsDao(DAppSettingsDao appSettingsDao) {
        this.appSettingsDao = appSettingsDao;
    }

    public void setFeedbackDao(DFeedbackDao feedbackDao) {
        this.feedbackDao = feedbackDao;
    }

    public void setToEmail(String toEmail) {
        this.toEmail = toEmail;
    }

    public void setFromEmail(String fromEmail) {
        this.fromEmail = fromEmail;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public boolean isTracking() {
        return tracking;
    }

    public void setTracking(boolean tracking) {
        this.tracking = tracking;
    }

    public DQuestionDao getQuestionDao() {
        return questionDao;
    }

    public void setQuestionDao(DQuestionDao questionDao) {
        this.questionDao = questionDao;
    }

}
