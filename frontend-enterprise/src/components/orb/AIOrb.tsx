import React, { useRef, useMemo } from 'react';
import { Canvas, useFrame } from '@react-three/fiber';
import * as THREE from 'three';

interface OrbMeshProps {
  streaming: boolean;
}

function OrbMesh({ streaming }: OrbMeshProps) {
  const meshRef = useRef<THREE.Mesh>(null);
  const particlesRef = useRef<THREE.Points>(null);
  const timeRef = useRef(0);

  const particles = useMemo(() => {
    const count = 80;
    const positions = new Float32Array(count * 3);
    for (let i = 0; i < count; i++) {
      const theta = Math.random() * Math.PI * 2;
      const phi = Math.acos(2 * Math.random() - 1);
      const r = 0.6 + Math.random() * 0.4;
      positions[i * 3] = r * Math.sin(phi) * Math.cos(theta);
      positions[i * 3 + 1] = r * Math.sin(phi) * Math.sin(theta);
      positions[i * 3 + 2] = r * Math.cos(phi);
    }
    return positions;
  }, []);

  useFrame((_, delta) => {
    timeRef.current += delta;

    if (meshRef.current) {
      meshRef.current.rotation.y += streaming ? delta * 2.0 : delta * 0.3;
      meshRef.current.rotation.x = Math.sin(timeRef.current * 0.5) * 0.1;

      if (streaming) {
        const scale = 1 + Math.sin(timeRef.current * 4) * 0.08;
        meshRef.current.scale.setScalar(scale);
      } else {
        meshRef.current.scale.setScalar(1);
      }
    }

    if (particlesRef.current) {
      particlesRef.current.rotation.y -= delta * 0.5;
      particlesRef.current.visible = streaming;
    }
  });

  return (
    <>
      <pointLight position={[0, 2, 2]} intensity={2} color="#19D3FF" />
      <pointLight position={[0, -2, -1]} intensity={0.5} color="#6AE3FF" />
      <ambientLight intensity={0.3} />

      <mesh ref={meshRef}>
        <sphereGeometry args={[0.38, 32, 32]} />
        <meshStandardMaterial
          color="#050505"
          emissive="#19D3FF"
          emissiveIntensity={streaming ? 0.6 : 0.25}
          roughness={0.1}
          metalness={0.8}
          wireframe={false}
        />
      </mesh>

      {/* Outer glow ring */}
      <mesh rotation={[Math.PI / 2, 0, 0]}>
        <torusGeometry args={[0.5, 0.01, 8, 64]} />
        <meshStandardMaterial
          color="#19D3FF"
          emissive="#19D3FF"
          emissiveIntensity={streaming ? 1.5 : 0.5}
          transparent
          opacity={streaming ? 0.8 : 0.4}
        />
      </mesh>

      {/* Particles */}
      <points ref={particlesRef} visible={streaming}>
        <bufferGeometry>
          <bufferAttribute
            attach="attributes-position"
            args={[particles, 3]}
          />
        </bufferGeometry>
        <pointsMaterial
          size={0.015}
          color="#19D3FF"
          transparent
          opacity={0.7}
          sizeAttenuation
        />
      </points>
    </>
  );
}

interface AIOrbProps {
  streaming?: boolean;
  size?: number;
}

export function AIOrb({ streaming = false, size = 60 }: AIOrbProps) {
  return (
    <div
      style={{ width: size, height: size }}
      className="relative"
    >
      {/* Glow backdrop */}
      <div
        className="absolute inset-0 rounded-full"
        style={{
          background: streaming
            ? 'radial-gradient(circle, rgba(25,211,255,0.25) 0%, transparent 70%)'
            : 'radial-gradient(circle, rgba(25,211,255,0.1) 0%, transparent 70%)',
          transition: 'background 0.5s ease',
        }}
      />
      <Canvas
        camera={{ position: [0, 0, 2], fov: 50 }}
        gl={{ antialias: true, alpha: true }}
        style={{ background: 'transparent' }}
      >
        <OrbMesh streaming={streaming} />
      </Canvas>
    </div>
  );
}
