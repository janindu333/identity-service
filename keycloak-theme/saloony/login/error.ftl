<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Saloony - Error</title>
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
      border: 1px solid #3c2b36;
      border-radius: 14px;
      box-shadow: 0 20px 45px rgba(0, 0, 0, 0.45);
      overflow: hidden;
    }
    .head {
      padding: 22px 26px;
      border-bottom: 1px solid #3c2b36;
      background: linear-gradient(135deg, #26151b 0%, #1d1218 100%);
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
      font-size: 32px;
      line-height: 1.1;
      color: #ffffff;
    }
    .body {
      padding: 24px 26px 26px;
      color: #f0cfd6;
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
  </style>
</head>
<body>
  <div class="card">
    <div class="head">
      <div class="brand">Saloony</div>
      <h1>${msg("errorTitle")!"Something went wrong"}</h1>
    </div>
    <div class="body">
      <#if message?has_content>
      ${message.summary}
      </#if>
      <#if client?? && client.baseUrl??>
      <p><a href="${client.baseUrl}">${msg("backToApplication")!"Back to application"}</a></p>
      </#if>
    </div>
  </div>
</body>
</html>
