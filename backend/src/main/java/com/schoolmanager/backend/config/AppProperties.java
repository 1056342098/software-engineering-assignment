package com.schoolmanager.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {
	private final Jwt jwt = new Jwt();
	private final Crypto crypto = new Crypto();
	private final Storage storage = new Storage();
	private final Llm llm = new Llm();

	public Jwt getJwt() {
		return jwt;
	}

	public Crypto getCrypto() {
		return crypto;
	}

	public Storage getStorage() {
		return storage;
	}

	public Llm getLlm() {
		return llm;
	}

	public static class Jwt {
		private String secret;
		private long ttlSeconds = 86400;

		public String getSecret() {
			return secret;
		}

		public void setSecret(String secret) {
			this.secret = secret;
		}

		public long getTtlSeconds() {
			return ttlSeconds;
		}

		public void setTtlSeconds(long ttlSeconds) {
			this.ttlSeconds = ttlSeconds;
		}
	}

	public static class Crypto {
		private String aesKeyBase64;

		public String getAesKeyBase64() {
			return aesKeyBase64;
		}

		public void setAesKeyBase64(String aesKeyBase64) {
			this.aesKeyBase64 = aesKeyBase64;
		}
	}

	public static class Storage {
		private String policyDir = "./data/policy-files";
		private String approvalDir = "./data/approval-files";

		public String getPolicyDir() {
			return policyDir;
		}

		public void setPolicyDir(String policyDir) {
			this.policyDir = policyDir;
		}

		public String getApprovalDir() {
			return approvalDir;
		}

		public void setApprovalDir(String approvalDir) {
			this.approvalDir = approvalDir;
		}
	}

	public static class Llm {
		private boolean enabled = false;
		private String baseUrl;
		private String apiKey;
		private String model;
		private String endpoint = "/v1/chat/completions";
		private int timeoutSeconds = 30;

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String getBaseUrl() {
			return baseUrl;
		}

		public void setBaseUrl(String baseUrl) {
			this.baseUrl = baseUrl;
		}

		public String getApiKey() {
			return apiKey;
		}

		public void setApiKey(String apiKey) {
			this.apiKey = apiKey;
		}

		public String getModel() {
			return model;
		}

		public void setModel(String model) {
			this.model = model;
		}

		public String getEndpoint() {
			return endpoint;
		}

		public void setEndpoint(String endpoint) {
			this.endpoint = endpoint;
		}

		public int getTimeoutSeconds() {
			return timeoutSeconds;
		}

		public void setTimeoutSeconds(int timeoutSeconds) {
			this.timeoutSeconds = timeoutSeconds;
		}
	}
}
