<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>${msg("emailVerificationSubject")}</title>
</head>
<body style="margin:0;padding:0;background-color:#0e0f14;font-family:Arial,Helvetica,sans-serif;color:#e8e8ee;">
  <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background-color:#0e0f14;padding:24px 12px;">
    <tr>
      <td align="center">
        <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="max-width:640px;background:#161823;border:1px solid #262a39;border-radius:14px;overflow:hidden;">
          <tr>
            <td style="padding:24px 28px;background:linear-gradient(135deg,#171a27 0%,#10131f 100%);border-bottom:1px solid #262a39;">
              <div style="font-size:12px;letter-spacing:0.12em;text-transform:uppercase;color:#ffb366;font-weight:700;">Saloony</div>
              <h1 style="margin:10px 0 0;font-size:28px;line-height:1.2;color:#ffffff;">Verify your email</h1>
            </td>
          </tr>
          <tr>
            <td style="padding:26px 28px;">
              <p style="margin:0 0 14px;font-size:15px;line-height:1.6;color:#d6d8e5;">Hi ${(user.firstName!'there')},</p>
              <p style="margin:0 0 18px;font-size:15px;line-height:1.7;color:#d6d8e5;">
                Welcome to <strong style="color:#ffffff;">Saloony</strong>. Confirm your email to secure your account and continue onboarding.
              </p>
              <table role="presentation" cellspacing="0" cellpadding="0" style="margin:0 0 18px;">
                <tr>
                  <td style="border-radius:10px;background:#ff8a1f;">
                    <a href="${link}" style="display:inline-block;padding:12px 22px;color:#111214;text-decoration:none;font-size:14px;font-weight:700;">Verify Email</a>
                  </td>
                </tr>
              </table>
              <p style="margin:0 0 8px;font-size:13px;line-height:1.6;color:#a9aec6;">Use this link as soon as possible.</p>
              <p style="margin:0;font-size:13px;line-height:1.6;color:#8d93b0;">If you didn't request this, you can safely ignore this message.</p>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>
