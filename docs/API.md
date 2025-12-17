# CampusCoin H5 æ¥å£æ–‡æ¡£

## è¿è¡Œç¯å¢ƒ
- Servlet å®¹å™¨ï¼šTomcat 9/10ï¼ˆæˆ–å…¼å®¹å®¹å™¨ï¼‰
- JDK 8+
- MySQL 8+

## éƒ¨ç½²ä¸ç«¯å£
- é…ç½®æ–‡ä»¶ï¼š`src/main/resources/application.yml`
  ```yaml
  server:
    port: 8081
  ```
- æ³¨æ„ï¼šæœ¬é¡¹ç›®ä½¿ç”¨å¤–éƒ¨ Servlet å®¹å™¨ï¼ˆTomcat ç­‰ï¼‰ï¼Œå®é™…ç«¯å£ä»ç”±å®¹å™¨ `conf/server.xml` æ§åˆ¶ã€‚è¯·å°† `application.yml` çš„ `server.port` ä¸ Tomcat Connector ç«¯å£ä¿æŒä¸€è‡´ï¼Œä¾‹å¦‚ï¼š
  ```xml
  <Connector port="8081" protocol="HTTP/1.1" connectionTimeout="20000" redirectPort="8443" />
  ```
- éƒ¨ç½²æ–¹å¼ï¼š`mvn clean package` åï¼Œå°†ç”Ÿæˆçš„ `campuscoin-h5.war` æ”¾å…¥ Tomcat `webapps/`ï¼Œæˆ–ä½¿ç”¨ IDE é…ç½®éƒ¨ç½²ã€‚
- å‰ç«¯ï¼ˆJSP H5ï¼‰ä¸åç«¯ Servlet åŒå®¹å™¨åŒç«¯å£ï¼Œå…¥å£ `http://<host>:<port>/campuscoin-h5/index.jsp`ï¼ˆè‹¥ä¸º ROOT éƒ¨ç½²ï¼Œåˆ™ `http://<host>:<port>/index.jsp`ï¼‰ã€‚

## æ•°æ®åº“
- åˆå§‹åŒ–è„šæœ¬ï¼š`db/schema.sql`
- è¿æ¥é…ç½®ï¼š`src/main/resources/application.yml` ä¸­ `datasource` æ®µè½ï¼š
  ```yaml
  datasource:
    url: jdbc:mysql://localhost:3306/campuscoin?useSSL=false&serverTimezone=UTC&characterEncoding=utf8
    username: root
    password: 123456
    driver: com.mysql.cj.jdbc.Driver
  ```

## è®¤è¯æ¥å£
### æ³¨å†Œ
- `POST /api/auth/register`
- `Content-Type: application/json`
- Body
  ```json
  {
    "teamName": "string",      // å”¯ä¸€å›¢é˜Ÿå
    "password": "string",      // >=6 ä½ï¼ŒBCrypt å­˜å‚¨
    "contactName": "string",   // è”ç³»äºº
    "contactPhone": "string"   // è”ç³»æ–¹å¼
  }
  ```
- å“åº”ï¼ˆ201ï¼‰
  ```json
  {
    "success": true,
    "message": "Team registered successfully",
    "data": {
      "teamName": "xxx",
      "balance": 500
    }
  }
  ```
- å¯èƒ½é”™è¯¯ï¼š400 å‚æ•°ä¸åˆæ³•ï¼›409 å›¢é˜Ÿåå·²å­˜åœ¨ï¼›500 æ³¨å†Œå¤±è´¥ã€‚

### ç™»å½•
- `POST /api/auth/login`
- `Content-Type: application/json`
- Body
  ```json
  {
    "teamName": "string",
    "password": "string"
  }
  ```
- å“åº”ï¼ˆ200ï¼‰
  ```json
  {
    "success": true,
    "message": "Login successful",
    "data": {
      "teamName": "xxx",
      "balance": 500
    }
  }
  ```
- å¯èƒ½é”™è¯¯ï¼š400 å‚æ•°ç¼ºå¤±ï¼›401 è®¤è¯å¤±è´¥ã€‚

## ä¼šè¯
- ç™»å½•æˆåŠŸååˆ›å»º HttpSessionï¼Œé”®ï¼š`teamId`ã€`teamName`ã€‚
- å¦‚éœ€é‰´æƒæ¥å£ï¼Œå¯æ£€æŸ¥ session æ˜¯å¦å­˜åœ¨ã€‚

## æ—¥å¿—
- ä½¿ç”¨ `java.util.logging`ï¼Œé…ç½®æ–‡ä»¶ `logging.properties`ã€‚

## å‰ç«¯å…¥å£ä¸ä¸»é¢˜
- é¡µé¢ï¼š`index.jsp`ï¼ˆå†…å«ç™»å½•/æ³¨å†Œè¡¨å•ã€ä¸»é¢˜åˆ‡æ¢ã€å“åº”å¼å¸ƒå±€ï¼‰ã€‚
- å‰ç«¯è°ƒç”¨ï¼š`fetch('/api/auth/...')` ä½¿ç”¨ JSON äº¤äº’ã€‚

## Öµ°àÈÎÎñ½Ó¿Ú

### ²éÑ¯Öµ°àÈÎÎñÁĞ±í
- `GET /api/duty-tasks`
- ĞèÒªµÇÂ¼£¨Session ÖĞĞèÒª teamId£©
- ÏìÓ¦£¨200£©
  ```json
  {
    "success": true,
    "message": "»ñÈ¡³É¹¦",
    "data": {
      "tasks": [
        {
          "id": 1,
          "taskName": "ÖÜÒ»Ç°Ì¨Öµ°à",
          "startTime": "2025-12-18T09:00:00",
          "endTime": "2025-12-18T17:00:00",
          "requiredPeople": 2,
          "rewardCoins": 50,
          "taskDesc": "¸ºÔğÇ°Ì¨½Ó´ı",
          "taskStatus": "NOT_STARTED",
          "signupCount": 1,
          "remaining": 1,
          "mySignupStatus": "PENDING"
        }
      ]
    }
  }
  ```
- ¿ÉÄÜ´íÎó£º401 Î´µÇÂ¼

### ±¨Ãû²Î¼ÓÖµ°àÈÎÎñ
- `POST /api/duty-tasks/{taskId}/signups`
- ĞèÒªµÇÂ¼
- ÏìÓ¦£¨200£©
  ```json
  {
    "success": true,
    "message": "±¨Ãû³É¹¦",
    "data": {
      "signup": {
        "id": 1,
        "taskId": 1,
        "teamId": 1,
        "signupStatus": "PENDING",
        "signedAt": "2025-12-17T18:00:00"
      }
    }
  }
  ```
- ¿ÉÄÜ´íÎó£º
  - 400 ÈÎÎñ²»´æÔÚ
  - 400 ÈÎÎñÒÑ½áÊø£¬ÎŞ·¨±¨Ãû
  - 400 ÄãÒÑ±¨Ãû¸ÃÈÎÎñ
  - 400 ±¨ÃûÈËÊıÒÑÂú
  - 401 Î´µÇÂ¼
  - 500 ±¨ÃûÊ§°Ü

### È¡Ïû±¨Ãû£¨ĞÂÔö£©
- `DELETE /api/duty-tasks/{taskId}/signups`
- ĞèÒªµÇÂ¼
- ÏìÓ¦£¨200£©
  ```json
  {
    "success": true,
    "message": "È¡Ïû±¨Ãû³É¹¦",
    "data": null
  }
  ```
- ¿ÉÄÜ´íÎó£º
  - 400 Î´ÕÒµ½±¨Ãû¼ÇÂ¼
  - 400 ¸Ã±¨ÃûÒÑÉóºË£¬ÎŞ·¨È¡Ïû
  - 400 ÈÎÎñÒÑ¿ªÊ¼£¬ÎŞ·¨È¡Ïû
  - 401 Î´µÇÂ¼
  - 500 È¡ÏûÊ§°Ü
- ËµÃ÷£ºÖ»ÄÜÈ¡Ïû´ıÉóºË£¨PENDING£©×´Ì¬µÄ±¨Ãû£¬ÇÒÈÎÎñÎ´¿ªÊ¼
