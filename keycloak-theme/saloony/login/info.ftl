<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Saloony</title>
  <style>
    body {
      margin: 0;
      min-height: 100vh;
      font-family: Arial, Helvetica, sans-serif;
      background: radial-gradient(circle at 20% 20%, #1a2136 0%, #0d111d 45%, #090c14 100%);
      color: #e9edf7;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 24px;
      box-sizing: border-box;
    }
    .card {
      width: 100%;
      max-width: 640px;
      background: linear-gradient(180deg, #161c2d 0%, #111727 100%);
      border: 1px solid #2a3045;
      border-radius: 14px;
      box-shadow: 0 20px 45px rgba(0, 0, 0, 0.45);
      overflow: hidden;
    }
    .head {
      padding: 22px 26px;
      border-bottom: 1px solid #2a3045;
      background: linear-gradient(135deg, #171a27 0%, #10131f 100%);
    }
    .brand {
      color: #ffb366;
      text-transform: uppercase;
      font-size: 12px;
      font-weight: 700;
      letter-spacing: 0.1em;
      margin-bottom: 8px;
    }
    h1 {
      margin: 0;
      font-size: 34px;
      line-height: 1.1;
      color: #ffffff;
    }
    .body {
      padding: 24px 26px 26px;
      color: #d6d8e5;
      font-size: 15px;
      line-height: 1.7;
    }
    .body a {
      display: inline-block;
      margin-top: 8px;
      background: #ff8a1f;
      border: 1px solid #ff8a1f;
      color: #111214;
      text-decoration: none;
      font-weight: 700;
      border-radius: 10px;
      padding: 10px 18px;
    }
    .body a:hover {
      background: #ffa247;
      border-color: #ffa247;
    }
  </style>
</head>
<body>
  <#assign signInUrl = "http://localhost:3000/signin">
  <#assign summaryText = "">
  <#if message?has_content && message.summary?has_content>
    <#assign summaryText = message.summary?lower_case>
  </#if>
  <#assign isCompleted = summaryText?contains("account has been updated") || summaryText?contains("email verified")>
  <div class="card">
    <div class="head">
      <div class="brand">Saloony</div>
      <h1>Action required</h1>
    </div>
    <div class="body">
      <#if message?has_content && message.summary?has_content>
      <p>${message.summary?no_esc}</p>
      </#if>
      <#if !isCompleted && requiredActions?? && (requiredActions?size > 0)>
      <p>Required actions:</p>
      <ul>
        <#list requiredActions as actionItem>
        <li>${actionItem}</li>
        </#list>
      </ul>
      </#if>
      <#if !isCompleted && actionUri?? && actionUri?is_string && actionUri?has_content>
      <p><a href="${actionUri}">Continue</a></p>
      <#elseif !isCompleted && skipLink?? && skipLink?is_string && skipLink?has_content>
      <p><a href="${skipLink}">Continue</a></p>
      <#elseif isCompleted>
      <p>Redirecting to sign in in 3 seconds...</p>
      <p><a href="${signInUrl}">Back to Sign in</a></p>
      </#if>
    </div>
  </div>
  <#if isCompleted>
  <script>
    setTimeout(function () {
      window.location.href = "${signInUrl?js_string}";
    }, 3000);
  </script>
  </#if>
</body>
</html>
