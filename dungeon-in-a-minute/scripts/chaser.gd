extends CharacterBody2D
## Chaser: walks straight at the player. Dies into a time shard.

signal died(at: Vector2)

var target: Node2D
var speed := Balance.CHASER_SPEED

var _hp := Balance.CHASER_HP
var _knockback := Vector2.ZERO
var _flash := 0.0
var _dead := false


func _ready() -> void:
	add_to_group("enemies")


func _physics_process(delta: float) -> void:
	var chase := Vector2.ZERO
	if is_instance_valid(target):
		chase = global_position.direction_to(target.global_position) * speed
	velocity = chase + _knockback
	_knockback = _knockback.move_toward(Vector2.ZERO, 1600.0 * delta)
	move_and_slide()

	if _flash > 0.0:
		_flash -= delta
		queue_redraw()


func take_damage(amount: int) -> void:
	if _dead:
		return
	_hp -= amount
	_flash = 0.08
	queue_redraw()
	if _hp <= 0:
		_dead = true
		died.emit(global_position)
		queue_free()


func knockback(impulse: Vector2) -> void:
	_knockback = impulse


func _draw() -> void:
	var r := Balance.CHASER_RADIUS
	var color := Color.WHITE if _flash > 0.0 else Color("ff4d6a")
	draw_colored_polygon(PackedVector2Array([
		Vector2(0, -r), Vector2(r, 0), Vector2(0, r), Vector2(-r, 0)]), color)
