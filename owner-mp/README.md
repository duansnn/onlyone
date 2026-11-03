# Owner Mall 顾客小程序

顾客用的微信小程序。商品、购物车、下单、订单、售后、评价、优惠券都走现有后端 `/api`。商家仍用网站 `/admin`。手机网页 `/m` 先留着，不从这里打开。

登录暂时用手机号和密码，和网站是同一个账号。微信一键登录要有小程序 AppID 和 AppSecret，下一步再接。

## 打开

1. 安装[微信开发者工具](https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html)。
2. 导入本目录 `owner-mp`。没有 AppID 时用测试号；有了之后改 `project.config.json` 里的 `appid`。
3. 本地设置里关闭「校验合法域名、web-view、TLS 版本以及 HTTPS 证书」。
4. 启动后端 `owner-server`（41452）。要看演示商品图，再开网站 dev（5273）。
5. 接口地址在 `config.js`。真机必须改成已备案的 https 域名，并配到小程序后台的 request、uploadFile、downloadFile 合法域名。

下单后订单停在「待确认」，店主在网站后台点「确认收款」。
