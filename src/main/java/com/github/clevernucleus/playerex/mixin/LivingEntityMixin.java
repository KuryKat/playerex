package com.github.clevernucleus.playerex.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.github.clevernucleus.dataattributes.api.DataAttributesAPI;
import com.github.clevernucleus.playerex.api.ExAPI;
import com.github.clevernucleus.playerex.api.event.LivingEntityEvents;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;

@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
	
	@Unique
	private int playerex_healTicks;
	
	@Unique
	private LivingEntity get() {
		return (LivingEntity)(Object)this;
	}
	
	@ModifyVariable(method = "heal", at = @At("HEAD"))
	private float onHealModified(float amount) {
		return LivingEntityEvents.INCOMING_HEAL.invoker().onHeal(this.get(), amount);
	}
	
	@Inject(method = "heal", at = @At("HEAD"), cancellable = true)
	private void onHeal(float amount, CallbackInfo info) {
		final boolean cancel = LivingEntityEvents.HEAL.invoker().onHeal(this.get(), amount);
		
		if(!cancel) {
			info.cancel();
		}
	}
	
	@Inject(method = "tick", at = @At("TAIL"))
	private void onTick(CallbackInfo info) {
		LivingEntity livingEntity = this.get();
		LivingEntityEvents.TICK.invoker().onTick(livingEntity);
		
		if(!livingEntity.world.isClient) {
			if(this.playerex_healTicks < 20) {
				this.playerex_healTicks++;
			} else {
				DataAttributesAPI.ifPresent(livingEntity, ExAPI.HEALTH_REGENERATION, (Object)null, value -> {
					if(value > 0.0F && livingEntity.getHealth() < livingEntity.getMaxHealth()) {
						livingEntity.heal(value);
					}
					
					return (Object)null;
				});
				
				this.playerex_healTicks = 0;
			}
		}
	}
	
	@ModifyVariable(method = "damage", at = @At("HEAD"), ordinal = 0)
	private float onDamageModified(float amount, DamageSource source) {
		return LivingEntityEvents.INCOMING_DAMAGE.invoker().onDamage(this.get(), source, amount);
	}
	
	@Inject(method = "damage", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/LivingEntity;despawnCounter:I", ordinal = 0), cancellable = true)
	private void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> info) {
		final boolean cancel = LivingEntityEvents.DAMAGE.invoker().onDamage(this.get(), source, amount);
		
		if(!cancel) {
			info.setReturnValue(false);
		}
	}
}