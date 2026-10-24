extends CharacterBody2D
## Move-only player. The weapon fires by itself at the nearest enemy in range.

signal hit

const ProjectileScene := preload("res://scenes/projectile.tscn")

var joystick: Node
var projectile_parent: Node

var _fire_cooldown := 0.0
var _invuln := 0.0

@onready var _hurtbox: Area2D = $Hurtbox


func _physics_process(delta: float) -> void:
	velocity = _input_vector() * Balance.PLAYER_SPEED
	move_and_slide()

	_fire_cooldown -= delta
	if _fire_cooldown <= 0.0:
		var target := _nearest_enemy()
		if target:
			_shoot(target)
			_fire_cooldown = Balance.FIRE_INTERVAL

	if _invuln > 0.0:
		_invuln -= delta
		queue_redraw()
	elif _hurtbox.has_overlapping_bodies():
		_take_hit()


func _input_vector() -> Vector2:
	if joystick and joystick.vector != Vector2.ZERO:
		return joystick.vector
	# Keyboard fallback for testing on desktop.
	var v := Vector2(
		float(Input.is_physical_key_pressed(KEY_D) or Input.is_physical_key_pressed(KEY_RIGHT))
			- float(Input.is_physical_key_pressed(KEY_A) or Input.is_physical_key_pressed(KEY_LEFT)),
		float(Input.is_physical_key_pressed(KEY_S) or Input.is_physical_key_pressed(KEY_DOWN))
			- float(Input.is_physical_key_pressed(KEY_W) or Input.is_physical_key_pressed(KEY_UP)))
	return v.normalized()


func _nearest_enemy() -> Node2D:
	var best: Node2D = null
	var best_dist := Balance.FIRE_RANGE * Balance.FIRE_RANGE
	for enemy in get_tree().get_nodes_in_group("enemies"):
		var d := global_position.distance_squared_to(enemy.global_position)
		if d < best_dist:
			best_dist = d
			best = enemy
	return best


func _shoot(target: Node2D) -> void:
	var p := ProjectileScene.instantiate()
	p.global_position = global_position
	p.direction = global_position.direction_to(target.global_position)
	projectile_parent.add_child(p)


func _take_hit() -> void:
	_invuln = Balance.HIT_INVULN
	for body in _hurtbox.get_overlapping_bodies():
		if body.has_method("knockback"):
			body.knockback(global_position.direction_to(body.global_position) * Balance.HIT_KNOCKBACK)
	hit.emit()


func _draw() -> void:
	var color := Color("5ee6ff")
	if _invuln > 0.0 and int(_invuln * 20.0) % 2 == 0:
		color = Color(1, 1, 1, 0.4)
	draw_circle(Vector2.ZERO, Balance.PLAYER_RADIUS, color)
	draw_arc(Vector2.ZERO, Balance.PLAYER_RADIUS + 4.0, 0.0, TAU, 32, Color(color, 0.4), 2.0, true)
