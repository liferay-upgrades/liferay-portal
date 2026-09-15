#set ($h1 = '#')
#set ($h2 = '##')
$h1 ${artifactId}

A Liferay module that mocks an external API and consumes it through a configurable client. Use it when the real API is unreachable, such as during an upgrade or on a developer machine, and switch to the real API later without touching any Java code.

$h2 Generated Classes

- `${package}.${className}APIClient` is the exported contract. Other modules inject it and never learn whether the data came from a mock or from the real API.
- `${package}.configuration.${className}APIConfiguration` holds the base URL, the token, the timeout, and the mock switch.
- `${package}.internal.${className}APIClientImpl` implements the contract. It reads a bundled response in mock mode and calls the configured host otherwise.
- `${package}.internal.${className}APIMockResponseUtil` resolves a request path to a bundled JSON file. Both the client and the servlet read mock responses through it.
- `${package}.internal.servlet.${className}APIMockServlet` publishes the same bundled responses over HTTP.

$h2 Mock Mode and Real Mode

While `mockEnabled` is `true`, the client returns the JSON bundled under `src/main/resources/mock-responses` and makes no network call. Set `mockEnabled` to `false` and the same client calls `baseURL` instead, sending the configured token as a bearer token and failing on any response other than HTTP 200.

Both values live in Control Panel > System Settings > Third Party > ${className} API Configuration, so switching between a mock and the real API is a configuration change rather than a code change.

$h2 The Mock Endpoint

The servlet serves the bundled responses over HTTP, which lets browsers and front end applications reach them as well:

```
GET http://localhost:8080/o/${artifactId}/customer
```

The request path maps to a file name, so `/customer` resolves to `mock-responses/customer.json`. A path that matches no bundled file returns HTTP 404, and a request that omits the configured token returns HTTP 401.

$h2 Adding a Mock Response

Drop a JSON file into `src/main/resources/mock-responses` and redeploy. No Java change is needed. A file named `orders.json` is served at `/o/${artifactId}/orders` and returned by `get("/orders")`.

$h2 Consuming the Client

The client package is exported, so any other module can inject the service:

```java
@Reference
private ${className}APIClient _apiClient;

JSONObject jsonObject = _apiClient.getJSONObject("/customer");
```