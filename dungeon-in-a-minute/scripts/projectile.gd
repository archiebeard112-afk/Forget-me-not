extends Area2D

var direction := Vector2.RIGHT

var _life := Balance.PROJECTILE_LIFETIME
var _spent := false


func _ready() -> void:
	body_entered.connect(_on_body_entered)


func _physics_process(delta: float) -> void:
	position += direction * Balance.PROJECTILE_SPEED * delta
	_life -= delta
	if _life <= 0.0:
		queue_free()


func _on_body_entered(body: Node) -> void:
	if _spent or not body.has_method("take_damage"):
		return
	_spent = true
	body.take_damage(Balance.PROJECTILE_DAMAGE)
	queue_free()


func _draw() -> void:
	draw_circle(Vector2.ZERO, 5.0, Color("ffd34d"))
