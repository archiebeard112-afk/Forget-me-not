extends Node2D
## Dropped by enemies. Walk over it to put time back on the clock.

signal collected(seconds: float)

var player: Node2D

var _age := 0.0


func _process(delta: float) -> void:
	_age += delta
	if _age >= Balance.SHARD_LIFETIME:
		queue_free()
		return
	if is_instance_valid(player):
		var d := global_position.distance_to(player.global_position)
		if d <= Balance.SHARD_PICKUP_RADIUS:
			collected.emit(Balance.SHARD_TIME)
			queue_free()
			return
		if d <= Balance.SHARD_MAGNET_RADIUS:
			global_position = global_position.move_toward(
				player.global_position, Balance.SHARD_MAGNET_SPEED * delta)
	queue_redraw()


func _draw() -> void:
	var left := Balance.SHARD_LIFETIME - _age
	if left < Balance.SHARD_BLINK_TIME and int(left * 8.0) % 2 == 0:
		return
	var bob := sin(_age * 6.0) * 2.0
	var s := 7.0
	draw_colored_polygon(PackedVector2Array([
		Vector2(0, -s * 1.5 + bob), Vector2(s, bob), Vector2(0, s * 1.5 + bob), Vector2(-s, bob)]),
		Color("8affc1"))
