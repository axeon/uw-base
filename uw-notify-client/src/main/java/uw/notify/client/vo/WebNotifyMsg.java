package uw.notify.client.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * WebNotifyMsg。
 *
 * @author axeon
 */
@Schema(title = "通知消息", description = "通知消息")
public class WebNotifyMsg implements Serializable{

	/**
	 * 用户ID
	 */
	@Schema(title = "用户ID", description = "用户ID")
	private long userId;

	/**
	 * 运营商编号
	 */
	@Schema(title = "运营商编号", description = "运营商编号")
	private long saasId;

	/**
	 * 商户编号
	 */
	@Schema(title = "商户编号", description = "商户编号")
	private long mchId;

	/**
	 * 用户类型
	 */
	@Schema(title = "用户类型", description = "用户类型")
	private int userType;

	/**
	 * 通知类型
	 */
	@Schema(title = "通知类型", description = "通知类型")
	private String notifyType;

	/**
	 * 消息标题
	 */
	@Schema(title = "消息标题", description = "消息标题")
	private String notifySubject;

	/**
	 * 消息内容
	 */
	@Schema(title = "消息内容", description = "消息内容")
	private String notifyContent;

	/**
	 * 消息数据
	 */
	@Schema(title = "消息数据", description = "消息数据")
	private Object notifyData;

	private WebNotifyMsg(Builder builder) {
		setUserId( builder.userId );
		setSaasId( builder.saasId );
		setMchId( builder.mchId );
		setUserType( builder.userType );
		setNotifyType( builder.notifyType );
		setNotifySubject( builder.notifySubject );
		setNotifyContent( builder.notifyContent );
		setNotifyData( builder.notifyData );
	}

	public static Builder builder(WebNotifyMsg copy) {
		Builder builder = new Builder();
		builder.userId = copy.getUserId();
		builder.saasId = copy.getSaasId();
		builder.mchId = copy.getMchId();
		builder.userType = copy.getUserType();
		builder.notifyType = copy.getNotifyType();
		builder.notifySubject = copy.getNotifySubject();
		builder.notifyContent = copy.getNotifyContent();
		builder.notifyData = copy.getNotifyData();
		return builder;
	}

	public long getUserId() {
		return userId;
	}

	public void setUserId(long userId) {
		this.userId = userId;
	}

	public long getSaasId() {
		return saasId;
	}

	public void setSaasId(long saasId) {
		this.saasId = saasId;
	}

	public long getMchId() {
		return mchId;
	}

	public void setMchId(long mchId) {
		this.mchId = mchId;
	}

	public int getUserType() {
		return userType;
	}

	public void setUserType(int userType) {
		this.userType = userType;
	}

	public String getNotifyType() {
		return notifyType;
	}

	public void setNotifyType(String notifyType) {
		this.notifyType = notifyType;
	}

	public String getNotifySubject() {
		return notifySubject;
	}

	public void setNotifySubject(String notifySubject) {
		this.notifySubject = notifySubject;
	}

	public String getNotifyContent() {
		return notifyContent;
	}

	public void setNotifyContent(String notifyContent) {
		this.notifyContent = notifyContent;
	}

	public Object getNotifyData() {
		return notifyData;
	}

	public void setNotifyData(Object notifyData) {
		this.notifyData = notifyData;
	}

	public static final class Builder {
		private long userId;
		private long saasId;
		private long mchId;
		private int userType;
		private String notifyType;
		private String notifySubject;
		private String notifyContent;
		private Object notifyData;

		private Builder() {
		}

		public static Builder builder() {
			return new Builder();
		}

		public Builder userId(long userId) {
			this.userId = userId;
			return this;
		}

		public Builder saasId(long saasId) {
			this.saasId = saasId;
			return this;
		}

		public Builder mchId(long mchId) {
			this.mchId = mchId;
			return this;
		}

		public Builder userType(int userType) {
			this.userType = userType;
			return this;
		}

		public Builder notifyType(String notifyType) {
			this.notifyType = notifyType;
			return this;
		}

		public Builder notifySubject(String notifySubject) {
			this.notifySubject = notifySubject;
			return this;
		}

		public Builder notifyContent(String notifyContent) {
			this.notifyContent = notifyContent;
			return this;
		}

		public Builder notifyData(Object notifyData) {
			this.notifyData = notifyData;
			return this;
		}

		public WebNotifyMsg build() {
			return new WebNotifyMsg( this );
		}
	}
}