# Bầu Cua Tôm Cá - Phiên bản OCSF Desktop (Version 1)

Dự án này là  trò chơi Bầu Cua Tôm Cá, sử dụng framework **OCSF (Object Client-Server Framework)** .

## 📂 Cấu trúc thư mục (Clean)
```text
baucua-java/
├── assets/
│   ├── audio/
│   │   ├── bgm.wav      (Nhạc nền)
│   │   └── roll.wav     (Hiệu ứng lắc)
│   └── images/
│       ├── background.png
│       ├── bau.png, cua.png, tom.png... (6 linh vật)
│       └── tieude.png, btn_play.png... (UI Elements)
├── bin/ (Thư mục thực thi - Compiled)
│   ├── game/
│   │   ├── BauCuaClient.class
│   │   ├── BauCuaGUI.class
│   │   └── BauCuaServer.class
│   ├── models/
│   │   └── DiceSymbol.class
│   └── ocsf/
│       ├── client/AbstractClient.class
│       └── server/AbstractServer.class, ConnectionToClient.class
├── src/ (Mã nguồn - Source Code)
│   ├── game/
│   │   ├── BauCuaClient.java
│   │   ├── BauCuaGUI.java
│   │   ├── BauCuaServer.java
│   │   └── DiceEngine.java
│   ├── models/
│   │   └── DiceSymbol.java
│   └── ocsf/
│       ├── client/AbstractClient.java
│       └── server/AbstractServer.java, ConnectionToClient.java
└── README.md
```

## 🚀 Chạy dự án bằng VS Code

Mở terminal trong VS Code 

### 1. Chạy Server
```powershell
java -cp bin game.BauCuaServer 5000
```

### 2. Chạy Client (Giao diện Game)
```powershell
java -cp bin game.BauCuaGUI localhost 5000
```



