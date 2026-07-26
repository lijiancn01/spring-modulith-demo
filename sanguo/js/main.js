// 三国群英传 - 游戏入口和主循环
window.SG = window.SG || {};

(function() {
  'use strict';

  var Main = {

    // 游戏循环定时器
    _loopTimer: null,

    // 是否正在运行
    _running: false,

    // ===== 初始化 =====
    init: function() {
      // 1. 初始化游戏状态
      window.SG.GameState.init();

      // 2. 创建地图渲染器和控制器
      if (window.SG.MapRenderer) {
        window.SG.MapRenderer.init();
      }
      var canvas = document.getElementById('gameCanvas');
      if (canvas && window.SG.MapController) {
        window.SG.MapController.init(canvas);
      }

      // 3. 创建UI管理器
      if (window.SG.UIManager) {
        window.SG.UIManager.init();
      }

      // 4. 显示开始画面
      this._showStartScreen();
    },

    // ===== 开始画面 =====
    _showStartScreen: function() {
      var container = document.getElementById('gameContainer') || document.body;

      var overlay = document.createElement('div');
      overlay.id = 'startScreen';
      overlay.style.cssText = [
        'position: fixed',
        'top: 0; left: 0',
        'width: 100%; height: 100%',
        'background: linear-gradient(135deg, #1a0a00 0%, #3d1a00 50%, #1a0a00 100%)',
        'display: flex',
        'flex-direction: column',
        'align-items: center',
        'justify-content: center',
        'z-index: 9999',
        'font-family: "Microsoft YaHei", "SimHei", sans-serif',
        'color: #e8d4b0'
      ].join(';');

      // 标题
      var title = document.createElement('h1');
      title.textContent = '三国群英传';
      title.style.cssText = [
        'font-size: 56px',
        'margin: 0 0 10px 0',
        'text-shadow: 0 0 20px #ff6600, 0 0 40px #cc3300',
        'letter-spacing: 12px',
        'color: #ffd700'
      ].join(';');
      overlay.appendChild(title);

      // 副标题
      var subtitle = document.createElement('p');
      subtitle.textContent = '选择你的势力，逐鹿天下';
      subtitle.style.cssText = [
        'font-size: 18px',
        'margin: 0 0 40px 0',
        'color: #c4a882'
      ].join(';');
      overlay.appendChild(subtitle);

      // 势力选择
      var factions = [
        { id: 'wei', name: '曹魏', color: '#4488cc', desc: '挟天子以令诸侯，雄踞中原' },
        { id: 'shu', name: '蜀汉', color: '#cc4444', desc: '兴复汉室，还于旧都' },
        { id: 'wu',  name: '东吴', color: '#44aa44', desc: '据江东之固，虎视天下' }
      ];

      var factionArea = document.createElement('div');
      factionArea.style.cssText = 'display: flex; gap: 30px; margin-bottom: 40px;';

      for (var i = 0; i < factions.length; i++) {
        (function(f) {
          var btn = document.createElement('div');
          btn.style.cssText = [
            'width: 180px',
            'padding: 20px',
            'border: 2px solid ' + f.color,
            'border-radius: 8px',
            'text-align: center',
            'cursor: pointer',
            'background: rgba(0,0,0,0.5)',
            'transition: all 0.3s'
          ].join(';');

          var nameEl = document.createElement('div');
          nameEl.textContent = f.name;
          nameEl.style.cssText = 'font-size: 28px; color: ' + f.color + '; margin-bottom: 8px; font-weight: bold;';
          btn.appendChild(nameEl);

          var descEl = document.createElement('div');
          descEl.textContent = f.desc;
          descEl.style.cssText = 'font-size: 13px; color: #b0a080; line-height: 1.5;';
          btn.appendChild(descEl);

          btn.onmouseover = function() {
            btn.style.background = f.color + '33';
            btn.style.transform = 'scale(1.05)';
          };
          btn.onmouseout = function() {
            btn.style.background = 'rgba(0,0,0,0.5)';
            btn.style.transform = 'scale(1)';
          };
          btn.onclick = function() {
            Main._selectFaction(f.id);
          };

          factionArea.appendChild(btn);
        })(factions[i]);
      }
      overlay.appendChild(factionArea);

      // 继续游戏按钮
      var hasSave = false;
      try {
        hasSave = !!localStorage.getItem('sg_save_0');
      } catch(e) {}

      if (hasSave) {
        var loadBtn = document.createElement('div');
        loadBtn.textContent = '继续游戏';
        loadBtn.style.cssText = [
          'padding: 12px 40px',
          'border: 1px solid #8a7a5a',
          'border-radius: 6px',
          'cursor: pointer',
          'font-size: 18px',
          'color: #c4a882',
          'background: rgba(0,0,0,0.4)',
          'transition: all 0.3s'
        ].join(';');
        loadBtn.onmouseover = function() {
          loadBtn.style.background = 'rgba(100,80,50,0.4)';
        };
        loadBtn.onmouseout = function() {
          loadBtn.style.background = 'rgba(0,0,0,0.4)';
        };
        loadBtn.onclick = function() {
          if (window.SG.GameState.load(0)) {
            Main._closeStartScreen(overlay);
            Main._startGame();
          }
        };
        overlay.appendChild(loadBtn);
      }

      container.appendChild(overlay);
    },

    // 选择势力
    _selectFaction: function(factionId) {
      window.SG.GameState.playerFaction = factionId;
      window.SG.GameState.init();
      window.SG.GameState.playerFaction = factionId;

      // 关闭开始画面，启动游戏
      var overlay = document.getElementById('startScreen');
      this._closeStartScreen(overlay);
      this._startGame();
    },

    // 关闭开始画面
    _closeStartScreen: function(overlay) {
      if (overlay && overlay.parentNode) {
        overlay.parentNode.removeChild(overlay);
      }
    },

    // ===== 启动游戏 =====
    _startGame: function() {
      // 重新初始化地图尺寸（开始画面移除后布局变化）
      if (window.SG.MapRenderer) {
        window.SG.MapRenderer._resize();
        window.SG.MapRenderer._bgDirty = true;
        window.SG.MapRenderer.render();
      }

      // 更新UI
      if (window.SG.UIManager) {
        window.SG.UIManager.updateAll();
      }

      // 启动游戏主循环
      this._running = true;
      this._gameLoop();
    },

    // ===== 游戏主循环 =====
    _gameLoop: function() {
      if (!this._running) return;

      var GS = window.SG.GameState;

      // 战斗阶段由 BattleScene 自行驱动
      if (GS.phase === 'battle' && window.SG.BattleScene) {
        window.SG.BattleScene.update();
        window.SG.BattleScene.render();
      }

      // 战略阶段：等待玩家操作，由事件驱动，循环只做少量轮询
      if (GS.phase === 'strategic') {
        // 地图动画、UI刷新等
        if (window.SG.MapRenderer) {
          window.SG.MapRenderer.render();
        }
      }

      // 行军阶段：推进动画
      if (GS.phase === 'marching') {
        if (window.SG.MapRenderer) {
          window.SG.MapRenderer.render();
        }
      }

      this._loopTimer = requestAnimationFrame(function() {
        Main._gameLoop();
      });
    },

    // ===== 停止游戏 =====
    stop: function() {
      this._running = false;
      if (this._loopTimer) {
        cancelAnimationFrame(this._loopTimer);
        this._loopTimer = null;
      }
    }
  };

  window.SG.Main = Main;

  // 页面加载后自动初始化
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function() { Main.init(); });
  } else {
    Main.init();
  }

})();
