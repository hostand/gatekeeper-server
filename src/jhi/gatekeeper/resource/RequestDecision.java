package jhi.gatekeeper.resource;

import java.io.*;

/**
 * @author Sebastian Raubach
 */
public class RequestDecision extends LocaleRequest implements Serializable
{
	private Integer  requestId;
	private Decision decision;
	private String   feedback;

	public RequestDecision()
	{
	}

	public RequestDecision(Integer requestId, Decision decision, String feedback)
	{
		this.requestId = requestId;
		this.decision = decision;
		this.feedback = feedback;
	}

	public Integer getRequestId()
	{
		return requestId;
	}

	public RequestDecision setRequestId(Integer requestId)
	{
		this.requestId = requestId;
		return this;
	}

	public Decision getDecision()
	{
		return decision;
	}

	public RequestDecision setDecision(Decision decision)
	{
		this.decision = decision;
		return this;
	}

	public String getFeedback()
	{
		return feedback;
	}

	public RequestDecision setFeedback(String feedback)
	{
		this.feedback = feedback;
		return this;
	}
}
