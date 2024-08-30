package uw.tinyurl.client.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.Date;

/**
 * 短链数据
 *
 * @author axeon
 */
@Schema(title = "短链数据", description = "短链数据")
public class TinyurlParam implements Serializable{


	/**
	 * saasId
	 */
	@Schema(title = "saasId", description = "saasId")
	private long saasId;

	/**
	 * 对象类型
	 */
	@Schema(title = "对象类型", description = "对象类型")
	private String objectType;

	/**
	 * 对象Id
	 */
	@Schema(title = "对象Id", description = "对象Id")
	private long objectId;

	/**
	 * url
	 */
	@Schema(title = "url", description = "url")
	private String url;

	/**
	 * 密语提示？
	 */
	@Schema(title = "密语提示？", description = "密语提示？")
	private String secretTips;

	/**
	 * 密语
	 */
	@Schema(title = "密语", description = "密语")
	private String secretData;

	/**
	 * 过期时间
	 */
	@Schema(title = "过期时间", description = "过期时间")
	private Date expireDate;

	public TinyurlParam() {
	}

	private TinyurlParam(Builder builder) {
		setSaasId( builder.saasId );
		setObjectType( builder.objectType );
		setObjectId( builder.objectId );
		setUrl( builder.url );
		setSecretTips( builder.secretTips );
		setSecretData( builder.secretData );
		setExpireDate( builder.expireDate );
	}

	public static Builder builder() {
		return new Builder();
	}

	public static Builder builder(TinyurlParam copy) {
		Builder builder = new Builder();
		builder.saasId = copy.getSaasId();
		builder.objectType = copy.getObjectType();
		builder.objectId = copy.getObjectId();
		builder.url = copy.getUrl();
		builder.secretTips = copy.getSecretTips();
		builder.secretData = copy.getSecretData();
		builder.expireDate = copy.getExpireDate();
		return builder;
	}

	public long getSaasId() {
		return saasId;
	}

	public void setSaasId(long saasId) {
		this.saasId = saasId;
	}

	public String getObjectType() {
		return objectType;
	}

	public void setObjectType(String objectType) {
		this.objectType = objectType;
	}

	public long getObjectId() {
		return objectId;
	}

	public void setObjectId(long objectId) {
		this.objectId = objectId;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getSecretTips() {
		return secretTips;
	}

	public void setSecretTips(String secretTips) {
		this.secretTips = secretTips;
	}

	public String getSecretData() {
		return secretData;
	}

	public void setSecretData(String secretData) {
		this.secretData = secretData;
	}

	public Date getExpireDate() {
		return expireDate;
	}

	public void setExpireDate(Date expireDate) {
		this.expireDate = expireDate;
	}

	public static final class Builder {
		private long saasId;
		private String objectType;
		private long objectId;
		private String url;
		private String secretTips;
		private String secretData;
		private Date expireDate;

		private Builder() {
		}

		public Builder saasId(long saasId) {
			this.saasId = saasId;
			return this;
		}

		public Builder objectType(String objectType) {
			this.objectType = objectType;
			return this;
		}

		public Builder objectId(long objectId) {
			this.objectId = objectId;
			return this;
		}

		public Builder url(String url) {
			this.url = url;
			return this;
		}

		public Builder secretTips(String secretTips) {
			this.secretTips = secretTips;
			return this;
		}

		public Builder secretData(String secretData) {
			this.secretData = secretData;
			return this;
		}

		public Builder expireDate(Date expireDate) {
			this.expireDate = expireDate;
			return this;
		}

		public TinyurlParam build() {
			return new TinyurlParam( this );
		}
	}
}