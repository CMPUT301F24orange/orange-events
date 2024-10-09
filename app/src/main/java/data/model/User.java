package data.model;

/* Todo: Add in profile image functionality */

import com.google.firebase.firestore.DocumentId;

/**
 * This class defines a user
 */
public class User {
    @DocumentId
    private String id;
    private String profileName;
    private String role; // "entrant", "organizer", or "admin"
    private Boolean receiveOrganizerNotifications;
    private Boolean receiveAdminNotifications;

    /**
     * Default constructor required for Firestone
     * @see <a href="https://firebase.google.com/docs/firestore/manage-data/add-data#custom_objects">...</a>
     */
    public User() {
    }

    /**
     * Constructor
     * @param id
     * @param profileName
     * @param role
     * @param receiveOrganizerNotifications
     * @param receiveAdminNotifications
     */
    /* Todo:
    *   See how we deal with a user creation and what is mandatory */
    public User(String id, String profileName, String role, Boolean receiveOrganizerNotifications, Boolean receiveAdminNotifications) {
        this.id = id;
        this.profileName = profileName;
        this.role = role;
        this.receiveOrganizerNotifications = receiveOrganizerNotifications;
        this.receiveAdminNotifications = receiveAdminNotifications;
    }

    /* Todo: Complete the rest of the user model */


}
