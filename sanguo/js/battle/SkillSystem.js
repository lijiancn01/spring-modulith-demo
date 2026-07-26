// 三国群英传 - 技能执行系统
window.SG = window.SG || {};

(function() {
  'use strict';

  var SkillSystem = {

    // 执行技能
    // skillId: 技能ID
    // caster: 施放者武将数据（战斗状态中的英雄对象，含heroId引用原始数据）
    // targets: 目标数组（战斗状态中的英雄对象）
    // side: 'attacker' 或 'defender'，施放者所属方
    execute: function(skillId, caster, targets, side) {
      var skillData = window.SG.SKILLS_DATA[skillId];
      if (!skillData) return null;

      var GS = window.SG.GameState;
      var heroData = GS.heroes[caster.heroId];
      if (!heroData) return null;

      var result = {
        type: skillData.type,
        skillId: skillId,
        skillName: skillData.name,
        source: caster.heroId,
        sourceName: heroData.name,
        side: side,
        range: skillData.range,
        targets: [],
        damage: 0,
        heal: 0,
        animation: this._getAnimationType(skillId)
      };

      var i, target, targetHero, dmg, healAmt;

      if (skillData.range === 'single') {
        // 单体攻击：对第一个目标造成伤害
        if (targets.length > 0) {
          target = targets[0];
          targetHero = GS.heroes[target.heroId];
          // 伤害 = power * (caster.force / 50)
          dmg = Math.floor(skillData.power * (heroData.force / 50) * (0.9 + Math.random() * 0.2));
          // 先扣兵力，兵力为0时扣HP
          if (target.troops > 0) {
            var troopLoss = Math.min(target.troops, dmg);
            target.troops -= troopLoss;
            dmg -= troopLoss;
          }
          if (dmg > 0) {
            target.hp = Math.max(0, target.hp - dmg);
          }
          var totalDmg = troopLoss + (dmg > 0 ? dmg : 0);
          result.targets.push({ heroId: target.heroId, name: targetHero ? targetHero.name : '', damage: totalDmg, troopLoss: troopLoss, hpLoss: dmg > 0 ? dmg : 0 });
          result.damage = totalDmg;
        }
      } else if (skillData.range === 'area') {
        // 群体攻击：对所有目标造成伤害
        // 伤害 = power * (caster.intellect / 50)
        var baseDmg = Math.floor(skillData.power * (heroData.intellect / 50) * (0.9 + Math.random() * 0.2));
        for (i = 0; i < targets.length; i++) {
          target = targets[i];
          if (target.hp <= 0 && target.troops <= 0) continue;
          targetHero = GS.heroes[target.heroId];
          // 群体伤害略有波动
          dmg = Math.floor(baseDmg * (0.8 + Math.random() * 0.4));
          var tTroopLoss = 0;
          var tHpLoss = 0;
          if (target.troops > 0) {
            tTroopLoss = Math.min(target.troops, dmg);
            target.troops -= tTroopLoss;
            dmg -= tTroopLoss;
          }
          if (dmg > 0) {
            tHpLoss = dmg;
            target.hp = Math.max(0, target.hp - dmg);
          }
          result.targets.push({ heroId: target.heroId, name: targetHero ? targetHero.name : '', damage: tTroopLoss + tHpLoss, troopLoss: tTroopLoss, hpLoss: tHpLoss });
          result.damage += tTroopLoss + tHpLoss;
        }
      } else if (skillData.range === 'self') {
        // 自身技能：恢复兵力或SP
        if (skillId === 'yingzi' || skillId === 'keji') {
          // 恢复SP
          var spRestore = Math.floor(skillData.power * 0.4);
          caster.sp = Math.min(caster.maxSp, caster.sp + spRestore);
          result.heal = spRestore;
          result.targets.push({ heroId: caster.heroId, name: heroData.name, spRestore: spRestore });
        } else if (skillId === 'rende' || skillId === 'buxiu' || skillId === 'tuntian') {
          // 恢复兵力：power * charisma / 50
          healAmt = Math.floor(skillData.power * (heroData.charisma / 50));
          var oldTroops = caster.troops;
          caster.troops = Math.min(caster.maxTroops, caster.troops + healAmt);
          var actualHeal = caster.troops - oldTroops;
          result.heal = actualHeal;
          result.targets.push({ heroId: caster.heroId, name: heroData.name, heal: actualHeal, troopRestore: actualHeal });
        } else if (skillId === 'jiyi') {
          // 桃园结义：提升己方全体士气
          // 增加己方所有英雄士气
          var moraleBoost = Math.floor(skillData.power * 0.2);
          for (i = 0; i < targets.length; i++) {
            target = targets[i];
            if (target.hp <= 0 && target.troops <= 0) continue;
            targetHero = GS.heroes[target.heroId];
            target.morale = Math.min(200, target.morale + moraleBoost);
            result.targets.push({ heroId: target.heroId, name: targetHero ? targetHero.name : '', moraleBoost: moraleBoost });
          }
          result.heal = moraleBoost;
        } else {
          // 通用自身技能：小幅恢复HP
          healAmt = Math.floor(skillData.power * 0.3);
          var oldHp = caster.hp;
          caster.hp = Math.min(caster.maxHp, caster.hp + healAmt);
          var actualHeal2 = caster.hp - oldHp;
          result.heal = actualHeal2;
          result.targets.push({ heroId: caster.heroId, name: heroData.name, heal: actualHeal2, hpRestore: actualHeal2 });
        }
      }

      return result;
    },

    // 获取武将当前可用的技能列表
    getAvailableSkills: function(hero) {
      if (!hero || !hero.skills) return [];
      var available = [];
      for (var i = 0; i < hero.skills.length; i++) {
        var skillId = hero.skills[i];
        var skillData = window.SG.SKILLS_DATA[skillId];
        if (skillData && hero.sp >= skillData.spCost) {
          available.push(skillData);
        }
      }
      return available;
    },

    // 获取技能消耗
    getSkillCost: function(skillId) {
      var skillData = window.SG.SKILLS_DATA[skillId];
      return skillData ? skillData.spCost : 0;
    },

    // 根据技能ID判断动画类型
    _getAnimationType: function(skillId) {
      var skillData = window.SG.SKILLS_DATA[skillId];
      if (!skillData) return 'ink';

      // 火系技能
      if (skillId === 'huoshao' || skillId === 'fenghuo') return 'fire';
      // 雷系技能
      if (skillId === 'leiji') return 'lightning';
      // 墨系（默认）
      return 'ink';
    }
  };

  window.SG.SkillSystem = SkillSystem;

})();
