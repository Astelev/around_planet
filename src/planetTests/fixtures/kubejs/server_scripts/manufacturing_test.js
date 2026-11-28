ServerEvents.recipes(event => {
  event.custom({type:'around_planet:machining',machine:'printer',ingredients:[{item:'minecraft:coal',count:3}],results:[{id:'minecraft:diamond',count:2}],processingTime:4,energy_per_tick:10,orbital_processing_ticks:3}).id('kubejs:manufacturing_probe')
  event.replaceInput({id:'around_planet:machining/kube_probe'},'minecraft:coal','minecraft:charcoal')
  event.replaceOutput({id:'around_planet:machining/kube_probe'},'minecraft:diamond','minecraft:emerald')
  event.recipes.around_planet.machining(['2x minecraft:gold_ingot'],['2x minecraft:clay_ball'],'mill',6).processingTime(8).energyPerTick(15).id('kubejs:manufacturing_builder')
  event.remove({id:'around_planet:machining/kube_removed'})
})
