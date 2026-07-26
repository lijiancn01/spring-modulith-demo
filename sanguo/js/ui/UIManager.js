// 三国群英传 - UI面板管理器
window.SG = window.SG || {};

(function() {
  'use strict';

  var GS = function() { return window.SG.GameState; };
  var DS = function() { return window.SG.DialogSystem; };

  var UIManager = {

    // 当前显示的面板ID
    activePanel: null,

    // DOM引用缓存
    _dom: {
      topbar: null,
      sidePanel: null,
      bottomBar: null,
      centerOverlay: null,
      notification: null
    },

    // ===== 初始化：创建基础DOM结构 =====
    init: function() {
      var container = document.getElementById('gameContainer') || document.body;

      // 顶部信息栏
      this._dom.topbar = this._createTopbar();
      container.appendChild(this._dom.topbar);

      // 右侧面板容器（320px宽）
      this._dom.sidePanel = document.createElement('div');
      this._dom.sidePanel.className = 'sg-panel';
      this._dom.sidePanel.id = 'sgSidePanel';
      this._dom.sidePanel.style.display = 'none';
      container.appendChild(this._dom.sidePanel);

      // 底部操作栏
      this._dom.bottomBar = document.createElement('div');
      this._dom.bottomBar.className = 'sg-bottombar';
      this._dom.bottomBar.id = 'sgBottomBar';
      container.appendChild(this._dom.bottomBar);

      // 中央覆盖层（战斗、事件等）
      this._dom.centerOverlay = document.createElement('div');
      this._dom.centerOverlay.className = 'sg-overlay';
      this._dom.centerOverlay.id = 'sgCenterOverlay';
      this._dom.centerOverlay.style.display = 'none';
      container.appendChild(this._dom.centerOverlay);

      // 浮动消息容器
      this._dom.notification = document.createElement('div');
      this._dom.notification.id = 'sgNotificationContainer';
      this._dom.notification.style.cssText = 'position:fixed;top:60px;left:50%;transform:translateX(-50%);z-index:10000;pointer-events:none;';
      container.appendChild(this._dom.notification);

      this.updateAll();
    },

    // ===== 创建顶部信息栏 =====
    _createTopbar: function() {
      var bar = document.createElement('div');
      bar.className = 'sg-topbar';
      bar.id = 'sgTopbar';

      // 回合信息
      var turnInfo = document.createElement('span');
      turnInfo.className = 'sg-topbar-item';
      turnInfo.id = 'sgTurnInfo';
      bar.appendChild(turnInfo);

      // 势力名称
      var factionInfo = document.createElement('span');
      factionInfo.className = 'sg-topbar-item';
      factionInfo.id = 'sgFactionInfo';
      bar.appendChild(factionInfo);

      // 金币
      var goldInfo = document.createElement('span');
      goldInfo.className = 'sg-topbar-item';
      goldInfo.id = 'sgGoldInfo';
      bar.appendChild(goldInfo);

      // 粮食
      var foodInfo = document.createElement('span');
      foodInfo.className = 'sg-topbar-item';
      foodInfo.id = 'sgFoodInfo';
      bar.appendChild(foodInfo);

      // 按钮区域
      var btnArea = document.createElement('span');
      btnArea.className = 'sg-topbar-btns';

      // 结束回合按钮
      btnArea.appendChild(this._createButton('结束回合', function() {
        UIManager._onEndTurn();
      }, 'sg-btn sg-btn-primary'));

      // 存档按钮
      btnArea.appendChild(this._createButton('存档', function() {
        UIManager.showSaveLoadDialog('save');
      }, 'sg-btn'));

      // 读档按钮
      btnArea.appendChild(this._createButton('读档', function() {
        UIManager.showSaveLoadDialog('load');
      }, 'sg-btn'));

      bar.appendChild(btnArea);
      return bar;
    },

    // ===== 结束回合 =====
    _onEndTurn: function() {
      var gs = GS();
      // 1. 执行回合结束
      gs.endTurn();

      // 2. 处理AI结果（已在endTurn中执行）

      // 3. 检查军队到达（已在endTurn中处理）
      // 如果触发了战斗，BattleScene会自行启动
      if (gs.phase === 'battle') {
        this.showMessage('战斗开始！');
        // 隐藏侧面板，显示战斗界面
        this.hidePanel();
      }

      // 4. 更新UI
      this.updateAll();
      this.showMessage('第 ' + gs.turn + ' 回合');
    },

    // ===== 刷新所有显示数据 =====
    updateAll: function() {
      var gs = GS();
      var pf = gs.playerFaction;
      var faction = gs.factions[pf];
      var factionName = (window.SG.FACTION_NAMES && window.SG.FACTION_NAMES[pf]) || pf;

      // 顶部栏数据
      var turnEl = document.getElementById('sgTurnInfo');
      var factionEl = document.getElementById('sgFactionInfo');
      var goldEl = document.getElementById('sgGoldInfo');
      var foodEl = document.getElementById('sgFoodInfo');

      if (turnEl) turnEl.textContent = '第 ' + gs.turn + ' 回合';
      if (factionEl) factionEl.textContent = '势力：' + factionName;
      if (goldEl) goldEl.textContent = '金币：' + (faction ? faction.gold : 0);
      if (foodEl) foodEl.textContent = '粮食：' + (faction ? faction.food : 0);
    },

    // ===== 显示城市管理面板 =====
    showCityPanel: function(cityId) {
      var gs = GS();
      var city = gs.cities[cityId];
      if (!city) return;

      this.activePanel = 'city_' + cityId;
      var panel = this._dom.sidePanel;
      panel.style.display = 'block';
      panel.innerHTML = '';

      // 城市名称和势力
      var factionName = (window.SG.FACTION_NAMES && window.SG.FACTION_NAMES[city.faction]) || city.faction;
      var factionColor = (window.SG.FACTION_COLORS && window.SG.FACTION_COLORS[city.faction]) || '#888';
      var header = this._createPanel('sgCityPanel', city.name + '（' + factionName + '）', '');
      header.querySelector('.sg-panel-title').style.borderColor = factionColor;
      panel.appendChild(header);

      var content = header.querySelector('.sg-panel-content');
      if (!content) { content = document.createElement('div'); content.className = 'sg-panel-content'; header.appendChild(content); }

      // 城市属性
      var statsDiv = document.createElement('div');
      statsDiv.className = 'sg-city-info';
      statsDiv.innerHTML = [
        this._createBar('农业', city.agriculture, 100, '#6b8e23') ,
        this._createBar('商业', city.commerce, 100, '#daa520') ,
        this._createBar('士气', city.morale, 100, '#cd853f') ,
        this._createBar('防御', city.defense, 100, '#708090')
      ].join('');
      // 兵力
      var currentTroops = gs._getCityTotalTroops(cityId);
      statsDiv.innerHTML += '<div class="sg-stat-row">兵力：' + currentTroops + ' / ' + city.maxTroops + '</div>';
      content.appendChild(statsDiv);

      // 开发分配
      var devDiv = document.createElement('div');
      devDiv.className = 'sg-city-info';
      devDiv.innerHTML = '<div class="sg-section-title">开发分配</div>';

      // 农业开发
      var agriHero = city.developAssign.agriculture ? gs.heroes[city.developAssign.agriculture] : null;
      var agriText = agriHero ? agriHero.name + '（开发中）' : '未指派';
      devDiv.innerHTML += '<div class="sg-assign-row"><span>农业：</span><span class="sg-assign-name">' + agriText + '</span></div>';

      // 商业开发
      var commHero = city.developAssign.commerce ? gs.heroes[city.developAssign.commerce] : null;
      var commText = commHero ? commHero.name + '（开发中）' : '未指派';
      devDiv.innerHTML += '<div class="sg-assign-row"><span>商业：</span><span class="sg-assign-name">' + commText + '</span></div>';

      content.appendChild(devDiv);

      // 驻守武将列表
      var heroDiv = document.createElement('div');
      heroDiv.className = 'sg-hero-list';
      heroDiv.innerHTML = '<div class="sg-section-title">驻守武将</div>';

      for (var i = 0; i < city.heroes.length; i++) {
        var hero = gs.heroes[city.heroes[i]];
        if (!hero) continue;
        var card = document.createElement('div');
        card.className = 'sg-hero-card';
        card.innerHTML = '<span class="sg-hero-name">' + hero.name + '</span>' +
          '<span class="sg-hero-status">' + this._heroStatusText(hero) + '</span>' +
          '<span class="sg-hero-troops">兵' + hero.troops + '</span>';
        // 点击显示武将详情
        (function(hid) {
          card.onclick = function() { UIManager.showHeroPanel(hid); };
          card.style.cursor = 'pointer';
        })(hero.id);
        heroDiv.appendChild(card);
      }

      if (city.heroes.length === 0) {
        heroDiv.innerHTML += '<div class="sg-empty">无驻守武将</div>';
      }
      content.appendChild(heroDiv);

      // 相邻城市
      var adjDiv = document.createElement('div');
      adjDiv.className = 'sg-city-info';
      adjDiv.innerHTML = '<div class="sg-section-title">相邻城市</div>';
      var adjNames = [];
      for (var j = 0; j < city.adjacent.length; j++) {
        var adjCity = gs.cities[city.adjacent[j]];
        if (adjCity) {
          var adjColor = (window.SG.FACTION_COLORS && window.SG.FACTION_COLORS[adjCity.faction]) || '#888';
          adjNames.push('<span style="color:' + adjColor + '">' + adjCity.name + '</span>');
        }
      }
      adjDiv.innerHTML += '<div>' + adjNames.join('、') + '</div>';
      content.appendChild(adjDiv);

      // 操作按钮
      var actionsDiv = document.createElement('div');
      actionsDiv.className = 'sg-action-buttons';

      // 开发农业
      actionsDiv.appendChild(this._createButton('开发农业', function() {
        UIManager._showDevelopHeroSelect(cityId, 'agriculture');
      }, 'sg-btn sg-btn-primary'));

      // 开发商业
      actionsDiv.appendChild(this._createButton('开发商业', function() {
        UIManager._showDevelopHeroSelect(cityId, 'commerce');
      }, 'sg-btn sg-btn-primary'));

      // 征兵
      actionsDiv.appendChild(this._createButton('征兵', function() {
        UIManager.showRecruitDialog(cityId);
      }, 'sg-btn sg-btn-primary'));

      // 搜索
      actionsDiv.appendChild(this._createButton('搜索', function() {
        UIManager._showSearchHeroSelect(cityId);
      }, 'sg-btn sg-btn-primary'));

      // 训练
      actionsDiv.appendChild(this._createButton('训练', function() {
        UIManager._showTrainHeroSelect(cityId);
      }, 'sg-btn sg-btn-primary'));

      // 出兵
      actionsDiv.appendChild(this._createButton('出兵', function() {
        UIManager.showDispatchDialog(cityId);
      }, 'sg-btn sg-btn-danger'));

      // 取消农业开发
      if (city.developAssign.agriculture) {
        actionsDiv.appendChild(this._createButton('取消农业开发', function() {
          var result = window.SG.CityManager.cancelDevelop(cityId, 'agriculture');
          UIManager.showMessage(result.msg);
          UIManager.showCityPanel(cityId);
        }, 'sg-btn'));
      }

      // 取消商业开发
      if (city.developAssign.commerce) {
        actionsDiv.appendChild(this._createButton('取消商业开发', function() {
          var result = window.SG.CityManager.cancelDevelop(cityId, 'commerce');
          UIManager.showMessage(result.msg);
          UIManager.showCityPanel(cityId);
        }, 'sg-btn'));
      }

      content.appendChild(actionsDiv);

      // 关闭按钮
      var closeBtn = this._createButton('关闭', function() {
        UIManager.hidePanel();
      }, 'sg-btn');
      content.appendChild(closeBtn);
    },

    // 武将状态文字
    _heroStatusText: function(hero) {
      var map = { idle: '空闲', developing: '开发中', marching: '行军中', battling: '战斗中' };
      return map[hero.status] || hero.status;
    },

    // 显示开发武将选择
    _showDevelopHeroSelect: function(cityId, target) {
      var gs = GS();
      var city = gs.cities[cityId];
      if (!city) return;

      // 筛选空闲武将
      var heroIds = [];
      for (var i = 0; i < city.heroes.length; i++) {
        var hero = gs.heroes[city.heroes[i]];
        if (hero && hero.status === 'idle') {
          heroIds.push(hero.id);
        }
      }

      this.showHeroSelect(heroIds, function(selectedHeroId) {
        var result = window.SG.CityManager.develop(cityId, selectedHeroId, target);
        UIManager.showMessage(result.msg);
        UIManager.showCityPanel(cityId);
        UIManager.updateAll();
      });
    },

    // 显示搜索武将选择
    _showSearchHeroSelect: function(cityId) {
      var gs = GS();
      var city = gs.cities[cityId];
      if (!city) return;

      var heroIds = [];
      for (var i = 0; i < city.heroes.length; i++) {
        var hero = gs.heroes[city.heroes[i]];
        if (hero && hero.status === 'idle') {
          heroIds.push(hero.id);
        }
      }

      this.showHeroSelect(heroIds, function(selectedHeroId) {
        var result = window.SG.CityManager.search(cityId, selectedHeroId);
        if (result.ok) {
          DS().showNotification(result.msg, 'success');
        } else {
          DS().showNotification(result.msg, 'warning');
        }
        UIManager.showCityPanel(cityId);
        UIManager.updateAll();
      });
    },

    // 显示训练武将选择
    _showTrainHeroSelect: function(cityId) {
      var gs = GS();
      var city = gs.cities[cityId];
      if (!city) return;

      var heroIds = [];
      for (var i = 0; i < city.heroes.length; i++) {
        var hero = gs.heroes[city.heroes[i]];
        if (hero && hero.status === 'idle') {
          heroIds.push(hero.id);
        }
      }

      this.showHeroSelect(heroIds, function(selectedHeroId) {
        var result = window.SG.CityManager.train(cityId, selectedHeroId);
        UIManager.showMessage(result.msg);
        UIManager.showCityPanel(cityId);
        UIManager.updateAll();
      });
    },

    // ===== 显示武将详情面板 =====
    showHeroPanel: function(heroId) {
      var gs = GS();
      var hero = gs.heroes[heroId];
      if (!hero) return;

      this.activePanel = 'hero_' + heroId;
      var panel = this._dom.sidePanel;
      panel.style.display = 'block';
      panel.innerHTML = '';

      var factionColor = (window.SG.FACTION_COLORS && window.SG.FACTION_COLORS[hero.faction]) || '#888';
      var factionName = (window.SG.FACTION_NAMES && window.SG.FACTION_NAMES[hero.faction]) || hero.faction;

      var header = this._createPanel('sgHeroPanel', hero.name, '');
      header.querySelector('.sg-panel-title').style.borderColor = factionColor;
      panel.appendChild(header);

      var content = header.querySelector('.sg-panel-content');
      if (!content) { content = document.createElement('div'); content.className = 'sg-panel-content'; header.appendChild(content); }

      // 基本信息
      var infoDiv = document.createElement('div');
      infoDiv.className = 'sg-hero-card';
      infoDiv.innerHTML = '<div>势力：<span style="color:' + factionColor + '">' + factionName + '</span></div>' +
        '<div>等级：' + hero.level + '　经验：' + hero.exp + '/' + (hero.level * 100) + '</div>';
      content.appendChild(infoDiv);

      // 属性条
      var statsDiv = document.createElement('div');
      statsDiv.className = 'sg-hero-stats';
      statsDiv.innerHTML =
        this._createBar('武力', hero.force, 100, '#cc4444') +
        this._createBar('智力', hero.intellect, 100, '#4488cc') +
        this._createBar('政治', hero.politics, 100, '#daa520') +
        this._createBar('统率', hero.command, 100, '#44aa44') +
        this._createBar('魅力', hero.charisma, 100, '#cc8844') +
        this._createBar('忠诚', hero.loyalty, 100, '#cd853f');
      content.appendChild(statsDiv);

      // 技能列表
      var skillsDiv = document.createElement('div');
      skillsDiv.className = 'sg-hero-skills';
      skillsDiv.innerHTML = '<div class="sg-section-title">武将技</div>';

      var skills = window.SG.HeroManager ? window.SG.HeroManager.getSkills(heroId) : [];
      for (var i = 0; i < skills.length; i++) {
        var skill = skills[i];
        skillsDiv.innerHTML += '<div class="sg-skill-item"><span class="sg-skill-name">' + skill.name + '</span>' +
          '<span class="sg-skill-cost">技力' + skill.spCost + '</span>' +
          '<div class="sg-skill-desc">' + skill.desc + '</div></div>';
      }
      if (skills.length === 0) {
        skillsDiv.innerHTML += '<div class="sg-empty">无技能</div>';
      }
      content.appendChild(skillsDiv);

      // 军师技
      if (hero.advisorSkill) {
        var advSkill = window.SG.SKILLS_DATA ? window.SG.SKILLS_DATA[hero.advisorSkill] : null;
        if (advSkill) {
          var advDiv = document.createElement('div');
          advDiv.className = 'sg-hero-skills';
          advDiv.innerHTML = '<div class="sg-section-title">军师技</div>' +
            '<div class="sg-skill-item"><span class="sg-skill-name">' + advSkill.name + '</span>' +
            '<div class="sg-skill-desc">' + advSkill.desc + '</div></div>';
          content.appendChild(advDiv);
        }
      }

      // 当前状态
      var statusDiv = document.createElement('div');
      statusDiv.className = 'sg-hero-card';
      var locationName = '未知';
      if (hero.location && gs.cities[hero.location]) {
        locationName = gs.cities[hero.location].name;
      }
      statusDiv.innerHTML = '<div>兵力：' + hero.troops + '/' + hero.maxTroops + '</div>' +
        '<div>位置：' + locationName + '</div>' +
        '<div>状态：' + this._heroStatusText(hero) + '</div>' +
        '<div>HP：' + hero.hp + '/' + hero.maxHp + '　SP：' + hero.sp + '/' + hero.maxSp + '</div>';
      content.appendChild(statusDiv);

      // 关闭按钮
      content.appendChild(this._createButton('关闭', function() {
        UIManager.hidePanel();
      }, 'sg-btn'));
    },

    // ===== 武将选择弹窗 =====
    showHeroSelect: function(heroIds, callback) {
      var gs = GS();

      var content = '<div class="sg-hero-select-list">';
      for (var i = 0; i < heroIds.length; i++) {
        var hero = gs.heroes[heroIds[i]];
        if (!hero) continue;
        var factionColor = (window.SG.FACTION_COLORS && window.SG.FACTION_COLORS[hero.faction]) || '#888';
        content += '<div class="sg-hero-card sg-hero-select-item" data-hero-id="' + hero.id + '">' +
          '<span class="sg-hero-name" style="color:' + factionColor + '">' + hero.name + '</span>' +
          '<span class="sg-hero-stat">武' + hero.force + ' 智' + hero.intellect + ' 统' + hero.command + '</span>' +
          '<span class="sg-hero-troops">兵' + hero.troops + '</span>' +
          '</div>';
      }
      if (heroIds.length === 0) {
        content += '<div class="sg-empty">没有可选武将</div>';
      }
      content += '</div>';

      var buttons = [];
      if (heroIds.length === 0) {
        buttons.push({ text: '关闭', onClick: function() { DS().hide(); }, className: 'sg-btn' });
      }

      DS().show('选择武将', content, buttons);

      // 绑定点击事件
      var items = document.querySelectorAll('.sg-hero-select-item');
      for (var j = 0; j < items.length; j++) {
        (function(item) {
          item.onclick = function() {
            var hid = item.getAttribute('data-hero-id');
            DS().hide();
            if (callback && hid) callback(hid);
          };
          item.style.cursor = 'pointer';
        })(items[j]);
      }
    },

    // ===== 征兵对话框 =====
    showRecruitDialog: function(cityId) {
      var gs = GS();
      var city = gs.cities[cityId];
      if (!city) return;

      var faction = gs.factions[city.faction];
      if (!faction) { this.showMessage('城市无势力'); return; }

      var currentTroops = gs._getCityTotalTroops(cityId);
      var availableCapacity = city.maxTroops - currentTroops;
      if (availableCapacity <= 0) {
        this.showMessage('兵力已达上限');
        return;
      }

      var maxAmount = Math.min(availableCapacity, Math.floor(faction.gold / 0.5), Math.floor(faction.food / 0.3));
      if (maxAmount <= 0) {
        this.showMessage('资源不足，无法征兵');
        return;
      }

      var content = '<div class="sg-recruit-dialog">' +
        '<div class="sg-recruit-info">可用容量：' + availableCapacity + '</div>' +
        '<div class="sg-recruit-info">金币：' + faction.gold + '　粮食：' + faction.food + '</div>' +
        '<div class="sg-slider-row">' +
        '<label>征兵数量：</label>' +
        '<input type="range" id="sgRecruitSlider" min="1" max="' + maxAmount + '" value="1" class="sg-slider">' +
        '<span id="sgRecruitAmount">1</span>' +
        '</div>' +
        '<div id="sgRecruitCost" class="sg-recruit-cost">消耗：金币1，粮食1</div>' +
        '</div>';

      var self = this;
      var buttons = [
        {
          text: '确认征兵',
          onClick: function() {
            var slider = document.getElementById('sgRecruitSlider');
            var amount = slider ? parseInt(slider.value, 10) : 0;
            if (amount <= 0) return;
            var result = window.SG.CityManager.recruit(cityId, amount);
            DS().hide();
            UIManager.showMessage(result.msg);
            UIManager.showCityPanel(cityId);
            UIManager.updateAll();
          },
          className: 'sg-btn sg-btn-primary'
        },
        {
          text: '取消',
          onClick: function() { DS().hide(); },
          className: 'sg-btn'
        }
      ];

      DS().show('征兵', content, buttons);

      // 绑定滑块事件
      var slider = document.getElementById('sgRecruitSlider');
      var amountEl = document.getElementById('sgRecruitAmount');
      var costEl = document.getElementById('sgRecruitCost');
      if (slider) {
        slider.oninput = function() {
          var val = parseInt(slider.value, 10);
          if (amountEl) amountEl.textContent = val;
          if (costEl) costEl.textContent = '消耗：金币' + Math.floor(val * 0.5) + '，粮食' + Math.floor(val * 0.3);
        };
      }
    },

    // ===== 出兵对话框 =====
    showDispatchDialog: function(fromCityId) {
      var gs = GS();
      var fromCity = gs.cities[fromCityId];
      if (!fromCity) return;

      // 可选武将（空闲且有兵）
      var heroContent = '<div class="sg-dispatch-heroes">';
      heroContent += '<div class="sg-section-title">选择出征武将</div>';
      var availableHeroes = [];
      for (var i = 0; i < fromCity.heroes.length; i++) {
        var hero = gs.heroes[fromCity.heroes[i]];
        if (hero && hero.status === 'idle' && hero.troops > 0) {
          availableHeroes.push(hero);
          heroContent += '<div class="sg-hero-card sg-dispatch-hero-item">' +
            '<label><input type="checkbox" class="sg-dispatch-check" data-hero-id="' + hero.id + '" checked> ' +
            hero.name + '（兵' + hero.troops + '）</label></div>';
        }
      }
      heroContent += '</div>';

      if (availableHeroes.length === 0) {
        DS().show('出兵', '<div class="sg-empty">没有可出征的武将</div>', [
          { text: '关闭', onClick: function() { DS().hide(); }, className: 'sg-btn' }
        ]);
        return;
      }

      // 目标城市（相邻非己方城市）
      var targetContent = '<div class="sg-dispatch-targets">';
      targetContent += '<div class="sg-section-title">选择目标城市</div>';
      var targetCities = [];
      for (var j = 0; j < fromCity.adjacent.length; j++) {
        var adjCity = gs.cities[fromCity.adjacent[j]];
        if (adjCity && adjCity.faction !== fromCity.faction) {
          targetCities.push(adjCity);
          var adjColor = (window.SG.FACTION_COLORS && window.SG.FACTION_COLORS[adjCity.faction]) || '#888';
          targetContent += '<div class="sg-dispatch-target-item">' +
            '<label><input type="radio" name="sgDispatchTarget" value="' + adjCity.id + '"> ' +
            '<span style="color:' + adjColor + '">' + adjCity.name + '</span>（兵' + (gs._getCityTotalTroops(adjCity.id)) + '）</label></div>';
        }
      }
      targetContent += '</div>';

      if (targetCities.length === 0) {
        DS().show('出兵', '<div class="sg-empty">没有可进攻的相邻城市</div>', [
          { text: '关闭', onClick: function() { DS().hide(); }, className: 'sg-btn' }
        ]);
        return;
      }

      var summaryContent = '<div id="sgDispatchSummary" class="sg-dispatch-summary">出征兵力：0</div>';

      var fullContent = heroContent + targetContent + summaryContent;

      var buttons = [
        {
          text: '确认出兵',
          onClick: function() {
            // 获取选中的武将
            var checks = document.querySelectorAll('.sg-dispatch-check:checked');
            var selectedHeroIds = [];
            for (var k = 0; k < checks.length; k++) {
              selectedHeroIds.push(checks[k].getAttribute('data-hero-id'));
            }

            // 获取目标城市
            var radio = document.querySelector('input[name="sgDispatchTarget"]:checked');
            var targetCityId = radio ? radio.value : null;

            if (selectedHeroIds.length === 0) {
              UIManager.showMessage('请选择出征武将');
              return;
            }
            if (!targetCityId) {
              UIManager.showMessage('请选择目标城市');
              return;
            }

            var result = window.SG.CityManager.dispatchArmy(fromCityId, selectedHeroIds, targetCityId);
            DS().hide();
            UIManager.showMessage(result.msg);
            UIManager.showCityPanel(fromCityId);
            UIManager.updateAll();
          },
          className: 'sg-btn sg-btn-danger'
        },
        {
          text: '取消',
          onClick: function() { DS().hide(); },
          className: 'sg-btn'
        }
      ];

      DS().show('出兵 - ' + fromCity.name, fullContent, buttons);

      // 绑定checkbox事件更新总兵力
      var checks = document.querySelectorAll('.sg-dispatch-check');
      for (var m = 0; m < checks.length; m++) {
        checks[m].onchange = function() {
          UIManager._updateDispatchSummary();
        };
      }
      // 初始更新
      this._updateDispatchSummary();
    },

    // 更新出兵兵力汇总
    _updateDispatchSummary: function() {
      var gs = GS();
      var checks = document.querySelectorAll('.sg-dispatch-check:checked');
      var total = 0;
      for (var i = 0; i < checks.length; i++) {
        var hid = checks[i].getAttribute('data-hero-id');
        var hero = gs.heroes[hid];
        if (hero) total += hero.troops;
      }
      var summary = document.getElementById('sgDispatchSummary');
      if (summary) summary.textContent = '出征兵力：' + total;
    },

    // ===== 战斗结果面板 =====
    showBattleResult: function(result) {
      if (!result) return;

      var gs = GS();
      var winnerText = result.winner === 'attacker' ? '攻方获胜' : '守方获胜';

      var content = '<div class="sg-battle-result">' +
        '<div class="sg-result-winner">' + winnerText + '</div>';

      // 攻方伤亡
      content += '<div class="sg-section-title">攻方伤亡</div>';
      for (var i = 0; i < result.attackerCasualties.length; i++) {
        var ac = result.attackerCasualties[i];
        var hero = gs.heroes[ac.heroId];
        var name = hero ? hero.name : ac.heroId;
        var statusText = ac.captured ? '（被俘）' : '';
        content += '<div class="sg-casualty-item">' + name + '：兵' + ac.troops + ' HP' + ac.hp + statusText + '</div>';
      }

      // 守方伤亡
      content += '<div class="sg-section-title">守方伤亡</div>';
      for (var j = 0; j < result.defenderCasualties.length; j++) {
        var dc = result.defenderCasualties[j];
        var dHero = gs.heroes[dc.heroId];
        var dName = dHero ? dHero.name : dc.heroId;
        var dStatusText = dc.captured ? '（被俘）' : '';
        content += '<div class="sg-casualty-item">' + dName + '：兵' + dc.troops + ' HP' + dc.hp + dStatusText + '</div>';
      }

      content += '</div>';

      var buttons = [
        {
          text: '返回',
          onClick: function() {
            DS().hide();
            // 应用战斗结果
            if (window.SG.BattleEngine) {
              window.SG.BattleEngine.applyResult();
            }
            // 停止战斗场景
            if (window.SG.BattleScene) {
              window.SG.BattleScene.stop();
            }
            UIManager.updateAll();
          },
          className: 'sg-btn sg-btn-primary'
        }
      ];

      DS().show('战斗结束', content, buttons);
    },

    // ===== 存档/读档对话框 =====
    showSaveLoadDialog: function(mode) {
      var content = '<div class="sg-saveload">';
      for (var slot = 0; slot < 3; slot++) {
        var info = this._getSaveInfo(slot);
        content += '<div class="sg-save-slot">' +
          '<div class="sg-slot-label">档位 ' + (slot + 1) + '</div>' +
          '<div class="sg-slot-info">' + (info || '空') + '</div>' +
          '<div class="sg-slot-actions">';

        if (mode === 'save') {
          content += '<button class="sg-btn sg-btn-primary sg-slot-save" data-slot="' + slot + '">保存</button>';
        } else {
          if (info) {
            content += '<button class="sg-btn sg-btn-primary sg-slot-load" data-slot="' + slot + '">读取</button>';
          }
        }

        if (info) {
          content += ' <button class="sg-btn sg-btn-danger sg-slot-delete" data-slot="' + slot + '">删除</button>';
        }

        content += '</div></div>';
      }
      content += '</div>';

      var buttons = [
        { text: '关闭', onClick: function() { DS().hide(); }, className: 'sg-btn' }
      ];

      var title = mode === 'save' ? '存档' : '读档';
      DS().show(title, content, buttons);

      // 绑定按钮事件
      var self = this;
      var saveBtns = document.querySelectorAll('.sg-slot-save');
      for (var i = 0; i < saveBtns.length; i++) {
        (function(btn) {
          btn.onclick = function() {
            var s = parseInt(btn.getAttribute('data-slot'), 10);
            var ok = GS().save(s);
            DS().hide();
            self.showMessage(ok ? '存档成功' : '存档失败');
          };
        })(saveBtns[i]);
      }

      var loadBtns = document.querySelectorAll('.sg-slot-load');
      for (var j = 0; j < loadBtns.length; j++) {
        (function(btn) {
          btn.onclick = function() {
            var s = parseInt(btn.getAttribute('data-slot'), 10);
            var ok = GS().load(s);
            DS().hide();
            self.showMessage(ok ? '读档成功' : '读档失败');
            if (ok) self.updateAll();
          };
        })(loadBtns[j]);
      }

      var delBtns = document.querySelectorAll('.sg-slot-delete');
      for (var k = 0; k < delBtns.length; k++) {
        (function(btn) {
          btn.onclick = function() {
            var s = parseInt(btn.getAttribute('data-slot'), 10);
            try { localStorage.removeItem('sg_save_' + s); } catch(e) {}
            DS().hide();
            self.showMessage('存档已删除');
            self.showSaveLoadDialog(mode);
          };
        })(delBtns[k]);
      }
    },

    // 获取存档信息
    _getSaveInfo: function(slot) {
      try {
        var raw = localStorage.getItem('sg_save_' + slot);
        if (!raw) return null;
        var data = JSON.parse(raw);
        var factionName = (window.SG.FACTION_NAMES && window.SG.FACTION_NAMES[data.playerFaction]) || data.playerFaction;
        return '第' + data.turn + '回合 ' + factionName;
      } catch(e) {
        return null;
      }
    },

    // ===== 关闭面板 =====
    hidePanel: function() {
      this.activePanel = null;
      this._dom.sidePanel.style.display = 'none';
      this._dom.sidePanel.innerHTML = '';
    },

    // ===== 浮动消息 =====
    showMessage: function(text) {
      var container = this._dom.notification;
      if (!container) return;

      var msg = document.createElement('div');
      msg.className = 'sg-notification sg-notification-info';
      msg.textContent = text;
      container.appendChild(msg);

      // 3秒后淡出移除
      setTimeout(function() {
        msg.style.opacity = '0';
        msg.style.transition = 'opacity 0.5s';
        setTimeout(function() {
          if (msg.parentNode) msg.parentNode.removeChild(msg);
        }, 500);
      }, 2500);
    },

    // ===== 内部：创建面板DOM =====
    _createPanel: function(id, title, content) {
      var panel = document.createElement('div');
      panel.className = 'sg-panel-inner';
      panel.id = id;

      var titleEl = document.createElement('div');
      titleEl.className = 'sg-panel-title';
      titleEl.textContent = title;
      panel.appendChild(titleEl);

      var contentEl = document.createElement('div');
      contentEl.className = 'sg-panel-content';
      if (typeof content === 'string') {
        contentEl.innerHTML = content;
      }
      panel.appendChild(contentEl);

      return panel;
    },

    // ===== 内部：创建按钮 =====
    _createButton: function(text, onclick, className) {
      var btn = document.createElement('button');
      btn.className = className || 'sg-btn';
      btn.textContent = text;
      btn.onclick = onclick;
      return btn;
    },

    // ===== 内部：创建属性条HTML =====
    _createBar: function(label, value, max, color) {
      var percent = max > 0 ? Math.floor(value / max * 100) : 0;
      return '<div class="sg-stat-bar">' +
        '<span class="sg-stat-label">' + label + '</span>' +
        '<div class="sg-stat-track"><div class="sg-stat-fill" style="width:' + percent + '%;background:' + color + '"></div></div>' +
        '<span class="sg-stat-value">' + value + '</span>' +
        '</div>';
    }
  };

  window.SG.UIManager = UIManager;

})();
