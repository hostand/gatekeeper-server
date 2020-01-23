package jhi.gatekeeper.resource;

/**
 * @author Sebastian Raubach
 */
public enum StatusMessage
{
	BAD_REQUEST_MISSING_FIELDS("Required fields not provided."),
	CONFLICT_USER_ALREADY_HAS_ACCESS("User already has access."),
	CONFLICT_USER_ALREADY_REQUESTED_ACCESS("User has already requested access."),
	CONFLICT_USERNAME_EMAIL_ALREADY_IN_USE("Username or email address already in use."),
	FORBIDDEN_ACCESS_TO_OTHER_USER("Access to other user not allowed."),
	FORBIDDEN_INSUFFICIENT_PERMISSIONS("Operation not allowed for current user."),
	FORBIDDEN_INVALID_CREDENTIALS("Invalid username or password."),
	NOT_FOUND_ACTIVATION_KEY("Invalid activation key."),
	NOT_FOUND_ACTIVATION_REQUEST("No request with the given activation key found."),
	NOT_FOUND_ID("Id not provided."),
	NOT_FOUND_ID_OR_PAYLOAD("Id or payload not provided."),
	NOT_FOUND_INSTITUTION("Institution not found."),
	NOT_FOUND_PAYLOAD("Payload not provided."),
	NOT_FOUND_TOKEN("Token not provided."),
	NOT_FOUND_USER("User not found."),
	UNAVAILABLE_EMAIL("Failed to send emails.");

	private String description;

	StatusMessage(String description)
	{
		this.description = description;
	}

	public String getDescription()
	{
		return description;
	}
}
