package bot.wheretoeat.telegram;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserSessionTest {
    @Test
    void newSessionStartsInNoneStep() {
        UserSession session = new UserSession();

        assertEquals(UserSession.Step.NONE, session.getStep());
        assertFalse(session.isWritingReview());
    }

    @Test
    void nullStepIsConvertedToNone() {
        UserSession session = new UserSession();
        session.setStep(UserSession.Step.REVIEW_TEXT);
        session.setStep(null);

        assertEquals(UserSession.Step.NONE, session.getStep());
    }

    @Test
    void reviewLifecycleCanBeStartedAndCleared() {
        UserSession session = new UserSession();
        session.startReview("restaurant-1");

        assertTrue(session.isWritingReview());
        assertEquals(UserSession.Step.REVIEW_RATING, session.getStep());
        assertEquals("restaurant-1", session.getReviewRestaurantId());

        session.setReviewRating(5);
        session.setReviewDraftText("Nice place");
        session.setStep(UserSession.Step.REVIEW_TEXT);
        session.clearReview();

        assertFalse(session.isWritingReview());
        assertEquals(UserSession.Step.NONE, session.getStep());
        assertNull(session.getReviewRestaurantId());
        assertEquals(0, session.getReviewRating());
        assertNull(session.getReviewDraftText());
    }

    @Test
    void cancelCurrentStepClearsReviewOrOrdinaryStep() {
        UserSession session = new UserSession();
        session.startReview("r1");
        session.cancelCurrentStep();
        assertEquals(UserSession.Step.NONE, session.getStep());
        assertNull(session.getReviewRestaurantId());

        session.setStep(UserSession.Step.FILTER_CUISINE);
        session.cancelCurrentStep();
        assertEquals(UserSession.Step.NONE, session.getStep());
    }

    @Test
    void resetFiltersClearsFilterValuesButPreservesReviewFlow() {
        UserSession session = new UserSession();
        session.setCuisine("Japanese");
        session.setDish("Ramen");
        session.setMinCheck(500);
        session.setMaxCheck(1500);
        session.startReview("r1");
        session.resetFilters();

        assertNull(session.getCuisine());
        assertNull(session.getDish());
        assertEquals(0, session.getMinCheck());
        assertEquals(0, session.getMaxCheck());
        assertEquals(UserSession.Step.REVIEW_RATING, session.getStep());
    }

    @Test
    void searchResultsAreDefensivelyCopiedAndNullBecomesEmptyList() {
        UserSession session = new UserSession();
        session.setLastSearchResults(null);
        assertTrue(session.getLastSearchResults().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> session.getLastSearchResults().add(null));
    }

    @Test
    void sessionExpirationUsesStrictGreaterThanComparison() {
        UserSession session = new UserSession();
        session.touch(1000L);

        assertFalse(session.isExpired(2000L, 1000L));
        assertTrue(session.isExpired(2001L, 1000L));
    }
}