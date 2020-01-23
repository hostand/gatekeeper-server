package jhi.gatekeeper.resource;

/**
 * @author Sebastian Raubach
 */
public class ActivationRequest extends LocaleRequest
{
	private String activationKey;

	public ActivationRequest()
	{
	}

	public ActivationRequest(String activationKey)
	{
		this.activationKey = activationKey;
	}

	public String getActivationKey()
	{
		return activationKey;
	}

	public ActivationRequest setActivationKey(String activationKey)
	{
		this.activationKey = activationKey;
		return this;
	}
}
